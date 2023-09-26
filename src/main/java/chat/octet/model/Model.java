package chat.octet.model;


import chat.octet.model.beans.LlamaContext;
import chat.octet.model.beans.LlamaContextParams;
import chat.octet.model.beans.LlamaModel;
import chat.octet.model.beans.Token;
import chat.octet.model.exceptions.ModelException;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.parameters.ModelParameter;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Iterator;

/**
 * Llama model
 *
 * @author william
 * @since 1.0
 */
@Slf4j
public class Model implements AutoCloseable {

    private final LlamaModel llamaModel;
    @Getter
    private final LlamaContext llamaContext;
    private final LlamaContextParams llamaContextParams;

    //llama context parameters
    @Getter
    private final ModelParameter modelParams;
    @Getter
    private final int contextSize;
    @Getter
    private final int embeddingSize;
    @Getter
    private final int vocabSize;
    @Getter
    private final int tokenBOS;
    @Getter
    private final int tokenEOS;
    @Getter
    private final int tokenNL;
    @Getter
    private final int batchSize;
    @Getter
    private final int lastTokensSize;
    @Getter
    private final String modelName;

    public Model(ModelParameter modelParams) {
        Preconditions.checkNotNull(modelParams, "Model parameters cannot be null");
        Preconditions.checkNotNull(modelParams.getModelPath(), "Model file path cannot be null");

        if (!Files.exists(new File(modelParams.getModelPath()).toPath())) {
            throw new ModelException("Model file is not exists, please check the file path");
        }

        this.modelParams = modelParams;
        this.modelName = modelParams.getModelName();
        //setting context parameters
        this.llamaContextParams = LlamaService.getLlamaContextDefaultParams();
        settingLlamaContextParameters(modelParams);

        this.llamaModel = LlamaService.loadLlamaModelFromFile(modelParams.getModelPath(), this.llamaContextParams);
        if (this.llamaModel == null) {
            throw new ModelException("Load model failed");
        }

        //apple lora from file
        if (StringUtils.isNotBlank(modelParams.getLoraPath())) {
            if (!Files.exists(new File(modelParams.getLoraPath()).toPath())) {
                throw new ModelException("Lora model file is not exists, please check the file path");
            }
            int status = LlamaService.loadLoraModelFromFile(llamaModel, modelParams.getLoraPath(), modelParams.getLoraBase(), modelParams.getThreads());
            if (status != 0) {
                throw new ModelException(String.format("Failed to apply LoRA from lora path: %s to base path: %s", modelParams.getLoraPath(), modelParams.getLoraBase()));
            }
        }

        this.llamaContext = LlamaService.createNewContextWithModel(llamaModel, this.llamaContextParams);
        this.contextSize = LlamaService.getContextSize(llamaContext);
        this.embeddingSize = LlamaService.getEmbeddingSize(llamaContext);
        this.vocabSize = LlamaService.getVocabSize(llamaContext);
        this.tokenBOS = LlamaService.getTokenBOS(llamaContext);
        this.tokenEOS = LlamaService.getTokenEOS(llamaContext);
        this.tokenNL = LlamaService.getTokenNL(llamaContext);
        this.batchSize = modelParams.getBatchSize();
        this.lastTokensSize = modelParams.getLastNTokensSize() < 0 ? contextSize : modelParams.getLastNTokensSize();

        if (modelParams.isVerbose()) {
            String systemInfo = LlamaService.printSystemInfo();
            log.info(MessageFormat.format("system info: {0}", systemInfo));
        }
        log.info(MessageFormat.format("model parameters: {0}", modelParams));
    }

    private void settingLlamaContextParameters(ModelParameter modelParams) {
        this.llamaContextParams.ctx = modelParams.getContextSize();
        this.llamaContextParams.seed = modelParams.getSeed();
        this.llamaContextParams.gpuLayers = modelParams.getGpuLayers();
        this.llamaContextParams.f16KV = modelParams.isF16KV();
        this.llamaContextParams.logitsAll = modelParams.isLogitsAll();
        this.llamaContextParams.vocabOnly = modelParams.isVocabOnly();
        this.llamaContextParams.embedding = modelParams.isEmbedding();
        this.llamaContextParams.lowVram = modelParams.isLowVram();
        this.llamaContextParams.ropeFreqBase = modelParams.getRopeFreqBase();
        this.llamaContextParams.ropeFreqScale = modelParams.getRopeFreqScale();
        boolean mmap = (StringUtils.isBlank(modelParams.getLoraPath()) && modelParams.isMmap());
        if (mmap && LlamaService.isMmapSupported()) {
            this.llamaContextParams.mmap = true;
        }
        boolean mlock = modelParams.isMlock();
        if (mlock && LlamaService.isMlockSupported()) {
            this.llamaContextParams.mlock = true;
        }
        if (modelParams.getMainGpu() != null) {
            this.llamaContextParams.mainGpu = modelParams.getMainGpu();
        }
        if (modelParams.getTensorSplit() != null) {
            this.llamaContextParams.tensorSplit = modelParams.getTensorSplit();
        }
        if (modelParams.getMulMatQ() != null) {
            this.llamaContextParams.mulMatQ = modelParams.getMulMatQ();
        }
    }

    public float[] getLogits() {
        return LlamaService.getLogits(llamaContext);
    }

    public Iterable<Token> generate(GenerateParameter generateParams, String text) {
        UserContext userContext = UserContextManager.getInstance().getDefaultUserContext(this);
        return generate(generateParams, userContext, text);
    }

    public Iterable<Token> generate(GenerateParameter generateParams, UserContext userContext, String text) {
        Preconditions.checkNotNull(generateParams, "Generate parameter cannot be null");
        Preconditions.checkNotNull(userContext, "User context cannot be null");
        Preconditions.checkNotNull(text, "Text cannot be null");

        return new Iterable<Token>() {

            private Generator generator = null;

            @Nonnull
            @Override
            public Iterator<Token> iterator() {
                if (generator == null) {
                    generator = new Generator(Model.this, generateParams, userContext, text);
                }
                return generator;
            }
        };
    }

    public void printTimings() {
        if (modelParams.isVerbose()) {
            LlamaService.printTimings(llamaContext);
        }
    }

    public float[] embedding(String text) {
        Preconditions.checkNotNull(text, "Text cannot be null");
        Preconditions.checkArgument(modelParams.isEmbedding(), "Llama model must be created with embedding=True to call this method");
        int[] tokens = tokenize(new String(text.getBytes(StandardCharsets.UTF_8)), true);
        evaluate(tokens, 0, tokens.length);
        float[] embedding = LlamaService.getEmbeddings(llamaContext);
        printTimings();
        return embedding;
    }

    public int[] tokenize(String text, boolean addBos) {
        int[] tokens = new int[getContextSize()];
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        int nextTokens = LlamaService.tokenizeWithModel(llamaModel, textBytes, textBytes.length, tokens, getContextSize(), addBos);
        if (nextTokens < 0) {
            throw new ModelException(String.format("failed to tokenize: %s, next_tokens: %s", text, nextTokens));
        }
        return ArrayUtils.subarray(tokens, 0, nextTokens);
    }

    protected int evaluate(int[] inputIds, int pastTokensSize, int inputLength) {
        int pastTokensTotal = pastTokensSize;

        int evaluateTotalSize;
        int evaluateSize;
        for (evaluateTotalSize = 0; pastTokensTotal < inputLength; evaluateTotalSize += evaluateSize) {
            evaluateSize = inputLength - pastTokensSize;
            if (evaluateSize > this.batchSize) {
                evaluateSize = this.batchSize;
            }

            int endIndex = evaluateSize + pastTokensSize;
            int[] batchTokens = ArrayUtils.subarray(inputIds, pastTokensSize, endIndex);
            int returnCode = LlamaService.evaluate(this.llamaContext, batchTokens, evaluateSize, pastTokensSize, this.modelParams.getThreads());
            if (returnCode != 0) {
                throw new ModelException("Llama_eval returned " + returnCode);
            }
            pastTokensTotal += evaluateSize;
        }
        return evaluateTotalSize;
    }

    protected int sampling(GenerateParameter generateParams, float[] logits, int[] inputIds, int inputLength) {
        int startIndex = Math.max(0, inputLength - getLastTokensSize());
        int[] lastTokens = ArrayUtils.subarray(inputIds, startIndex, inputLength);
        return LlamaService.sampling(
                llamaContext,
                logits,
                lastTokens,
                lastTokensSize,
                generateParams.getRepeatPenalty(),
                generateParams.getFrequencyPenalty(),
                generateParams.getPresencePenalty(),
                generateParams.isPenalizeNl(),
                generateParams.getMirostatMode().ordinal(),
                generateParams.getMirostatTAU(),
                generateParams.getMirostatETA(),
                generateParams.getTemperature(),
                generateParams.getTopK(),
                generateParams.getTopP(),
                generateParams.getTsf(),
                generateParams.getTypical()
        );
    }

    @Override
    public void close() {
        LlamaService.releaseLlamaContext(llamaContext);
        LlamaService.releaseLlamaModel(llamaModel);
    }

    @Override
    public String toString() {
        return "LlamaModel (" +
                "modelParams=" + modelParams +
                ')';
    }

}
