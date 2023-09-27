package chat.octet.model;


import chat.octet.model.beans.LlamaContextParams;
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
@Getter
@Slf4j
public class Model implements AutoCloseable {
    private final ModelParameter modelParams;
    private final int contextSize;
    private final int embeddingSize;
    private final int vocabSize;
    private final int tokenBOS;
    private final int tokenEOS;
    private final int tokenNL;
    private final int batchSize;
    private final int lastTokensSize;
    private final String modelName;

    public Model(String modelPath) {
        this(ModelParameter.builder().modelPath(modelPath).build());
    }

    public Model(ModelParameter modelParams) {
        Preconditions.checkNotNull(modelParams, "Model parameters cannot be null");
        Preconditions.checkNotNull(modelParams.getModelPath(), "Model file path cannot be null");

        if (!Files.exists(new File(modelParams.getModelPath()).toPath())) {
            throw new ModelException("Model file is not exists, please check the file path");
        }

        this.modelParams = modelParams;
        this.modelName = modelParams.getModelName();
        //setting context parameters
        LlamaContextParams llamaContextParams = settingLlamaContextParameters(modelParams);
        LlamaService.loadLlamaModelFromFile(modelParams.getModelPath(), llamaContextParams);

        //apple lora from file
        if (StringUtils.isNotBlank(modelParams.getLoraPath())) {
            if (!Files.exists(new File(modelParams.getLoraPath()).toPath())) {
                throw new ModelException("Lora model file is not exists, please check the file path");
            }
            int status = LlamaService.loadLoraModelFromFile(modelParams.getLoraPath(), modelParams.getLoraBase(), modelParams.getThreads());
            if (status != 0) {
                throw new ModelException(String.format("Failed to apply LoRA from lora path: %s to base path: %s", modelParams.getLoraPath(), modelParams.getLoraBase()));
            }
        }

        LlamaService.createNewContextWithModel(llamaContextParams);
        this.contextSize = LlamaService.getContextSize();
        this.embeddingSize = LlamaService.getEmbeddingSize();
        this.vocabSize = LlamaService.getVocabSize();
        this.tokenBOS = LlamaService.getTokenBOS();
        this.tokenEOS = LlamaService.getTokenEOS();
        this.tokenNL = LlamaService.getTokenNL();
        this.batchSize = modelParams.getBatchSize();
        this.lastTokensSize = modelParams.getLastNTokensSize() < 0 ? contextSize : modelParams.getLastNTokensSize();

        if (modelParams.isVerbose()) {
            log.info(MessageFormat.format("system info: {0}", LlamaService.getSystemInfo()));
        }
        log.info(MessageFormat.format("model parameters: {0}", modelParams));
    }

    private LlamaContextParams settingLlamaContextParameters(ModelParameter modelParams) {
        LlamaContextParams llamaContextParams = LlamaService.getLlamaContextDefaultParams();
        llamaContextParams.ctx = modelParams.getContextSize();
        llamaContextParams.seed = modelParams.getSeed();
        llamaContextParams.gpuLayers = modelParams.getGpuLayers();
        llamaContextParams.f16KV = modelParams.isF16KV();
        llamaContextParams.logitsAll = modelParams.isLogitsAll();
        llamaContextParams.vocabOnly = modelParams.isVocabOnly();
        llamaContextParams.embedding = modelParams.isEmbedding();
        llamaContextParams.lowVram = modelParams.isLowVram();
        llamaContextParams.ropeFreqBase = modelParams.getRopeFreqBase();
        llamaContextParams.ropeFreqScale = modelParams.getRopeFreqScale();
        boolean mmap = (StringUtils.isBlank(modelParams.getLoraPath()) && modelParams.isMmap());
        if (mmap && LlamaService.isMmapSupported()) {
            llamaContextParams.mmap = true;
        }
        boolean mlock = modelParams.isMlock();
        if (mlock && LlamaService.isMlockSupported()) {
            llamaContextParams.mlock = true;
        }
        if (modelParams.getMainGpu() != null) {
            llamaContextParams.mainGpu = modelParams.getMainGpu();
        }
        if (modelParams.getTensorSplit() != null) {
            llamaContextParams.tensorSplit = modelParams.getTensorSplit();
        }
        if (modelParams.getMulMatQ() != null) {
            llamaContextParams.mulMatQ = modelParams.getMulMatQ();
        }
        return llamaContextParams;
    }

    public String completions(GenerateParameter generateParams, String text) {
        Iterable<Token> tokenIterable = generate(generateParams, text);
        StringBuilder content = new StringBuilder();
        tokenIterable.forEach(token -> content.append(token.getText()));
        return content.toString();
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

    public void metrics() {
        if (modelParams.isVerbose()) {
            log.info("Metrics: " + LlamaService.getSamplingMetrics(true).toString());
        }
    }

    public float[] embedding(String text) {
        Preconditions.checkNotNull(text, "Text cannot be null");
        Preconditions.checkArgument(modelParams.isEmbedding(), "Llama model must be created with embedding=True to call this method");
        int[] tokens = tokenize(new String(text.getBytes(StandardCharsets.UTF_8)), true);
        evaluate(tokens, 0, tokens.length);
        float[] embedding = LlamaService.getEmbeddings();
        metrics();
        return embedding;
    }

    public int[] tokenize(String text, boolean addBos) {
        Preconditions.checkNotNull(text, "Text cannot be null");
        int[] tokens = new int[getContextSize()];
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        int nextTokens = LlamaService.tokenizeWithModel(textBytes, textBytes.length, tokens, getContextSize(), addBos);
        if (nextTokens < 0) {
            throw new ModelException(MessageFormat.format("failed to tokenize: {0}, next_tokens: {1}", text, nextTokens));
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
            int returnCode = LlamaService.evaluate(batchTokens, evaluateSize, pastTokensSize, this.modelParams.getThreads());
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
        LlamaService.release();
    }

    @Override
    public String toString() {
        return "LlamaModel (" +
                "modelParams=" + modelParams +
                ')';
    }

}
