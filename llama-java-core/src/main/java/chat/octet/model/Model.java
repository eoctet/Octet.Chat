package chat.octet.model;


import chat.octet.model.beans.CompletionResult;
import chat.octet.model.beans.LlamaContextParams;
import chat.octet.model.beans.LlamaModelParams;
import chat.octet.model.beans.Status;
import chat.octet.model.components.criteria.impl.StoppingWordCriteria;
import chat.octet.model.components.processor.impl.CustomBiasLogitsProcessor;
import chat.octet.model.enums.ModelType;
import chat.octet.model.exceptions.ModelException;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.parameters.ModelParameter;
import chat.octet.model.utils.PromptBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

/**
 * LLama model, which provides functions for generating and chatting conversations.
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */

@Slf4j
public class Model implements AutoCloseable {
    @Getter
    private final ModelParameter modelParams;
    @Getter
    private final String modelName;
    @Getter
    private final String modelType;
    private final Map<String, Status> chatStatus = Maps.newConcurrentMap();

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
        this.modelType = modelParams.getModelType();
        Preconditions.checkNotNull(modelType, "Model type cannot be null");
        //setting model parameters
        LlamaModelParams llamaModelParams = getLlamaModelParameters(modelParams);
        LlamaService.loadLlamaModelFromFile(modelParams.getModelPath(), llamaModelParams);
        //setting context parameters
        LlamaContextParams llamaContextParams = getLlamaContextParameters(modelParams);
        LlamaService.createNewContextWithModel(llamaContextParams);

        //apple lora from file
        if (StringUtils.isNotBlank(modelParams.getLoraPath())) {
            if (!Files.exists(new File(modelParams.getLoraPath()).toPath())) {
                throw new ModelException("Lora model file is not exists, please check the file path");
            }
            int status = LlamaService.loadLoraModelFromFile(modelParams.getLoraPath(), modelParams.getLoraScale(), modelParams.getLoraBase(), modelParams.getThreads());
            if (status != 0) {
                throw new ModelException(String.format("Failed to apply LoRA from lora path: %s to base path: %s", modelParams.getLoraPath(), modelParams.getLoraBase()));
            }
        }
        if (modelParams.isVerbose()) {
            log.info("system info: {}", LlamaService.getSystemInfo());
        }
        log.info("model parameters: {}", modelParams);
    }

    private LlamaModelParams getLlamaModelParameters(ModelParameter modelParams) {
        LlamaModelParams llamaModelParams = LlamaService.getLlamaModelDefaultParams();
        llamaModelParams.gpuLayers = modelParams.getGpuLayers();
        llamaModelParams.splitMode = modelParams.getSplitMode();
        llamaModelParams.vocabOnly = modelParams.isVocabOnly();
        boolean mmap = (StringUtils.isBlank(modelParams.getLoraPath()) && modelParams.isMmap());
        if (mmap && LlamaService.isMmapSupported()) {
            llamaModelParams.mmap = true;
        }
        boolean mlock = modelParams.isMlock();
        if (mlock && LlamaService.isMlockSupported()) {
            llamaModelParams.mlock = true;
        }
        if (modelParams.getMainGpu() != null) {
            llamaModelParams.mainGpu = modelParams.getMainGpu();
        }
        if (modelParams.getTensorSplit() != null) {
            llamaModelParams.tensorSplit = modelParams.getTensorSplit();
        }
        return llamaModelParams;
    }

    private LlamaContextParams getLlamaContextParameters(ModelParameter modelParams) {
        LlamaContextParams llamaContextParams = LlamaService.getLlamaContextDefaultParams();
        llamaContextParams.seed = modelParams.getSeed();
        llamaContextParams.ctx = modelParams.getContextSize();
        llamaContextParams.batch = modelParams.getBatchSize();
        llamaContextParams.threads = modelParams.getThreads();
        llamaContextParams.threadsBatch = modelParams.getThreadsBatch() == -1 ? modelParams.getThreads() : modelParams.getThreadsBatch();
        llamaContextParams.ropeScalingType = modelParams.getRopeScalingType();
        llamaContextParams.yarnExtFactor = modelParams.getYarnExtFactor();
        llamaContextParams.yarnAttnFactor = modelParams.getYarnAttnFactor();
        llamaContextParams.yarnBetaFast = modelParams.getYarnBetaFast();
        llamaContextParams.yarnBetaSlow = modelParams.getYarnBetaSlow();
        llamaContextParams.yarnOrigCtx = modelParams.getYarnOrigCtx();
        llamaContextParams.ropeFreqBase = modelParams.getRopeFreqBase();
        llamaContextParams.ropeFreqScale = modelParams.getRopeFreqScale();
        llamaContextParams.mulMatQ = modelParams.isMulMatQ();
        llamaContextParams.logitsAll = modelParams.isLogitsAll();
        llamaContextParams.embedding = modelParams.isEmbedding();
        llamaContextParams.offloadKqv = modelParams.isOffloadKqv();
        return llamaContextParams;
    }

    /**
     * Delete the session state of the specified user.
     *
     * @param session User session key.
     */
    public void removeChatStatus(String session) {
        String id = "";
        if (session.contains(":")) {
            id = session.split(":")[1];
        }
        for (String key : chatStatus.keySet()) {
            if (key.equals(session) || key.endsWith(id)) {
                Status status = chatStatus.remove(key);
                if (status != null) {
                    status.reset();
                }
                log.info("Removed chat session, session: {}.", key);
            }
        }
    }

    /**
     * Delete all user session states.
     */
    public void removeAllChatStatus() {
        int size = chatStatus.size();
        if (size > 0) {
            chatStatus.keySet().forEach(this::removeChatStatus);
            log.info("Removed all chat sessions, size: {}.", size);
        }
    }

    /**
     * Generate complete text.
     *
     * @param text Input text or prompt.
     * @return CompletionResult, generated text and completion reason.
     * @see CompletionResult
     */
    public CompletionResult completions(String text) {
        return completions(GenerateParameter.builder().build(), text);
    }

    /**
     * Generate complete text.
     *
     * @param generateParams Specify a generation parameter.
     * @param text           Input text or prompt.
     * @return CompletionResult, generated text and completion reason.
     * @see CompletionResult
     */
    public CompletionResult completions(GenerateParameter generateParams, String text) {
        return generate(generateParams, text).result();
    }

    /**
     * Generate text in stream format.
     *
     * @param text Input text or prompt.
     * @return Inference generator.
     * @see Generator
     */
    public Generator generate(String text) {
        return generate(GenerateParameter.builder().build(), text);
    }

    /**
     * Generate text in stream format.
     *
     * @param generateParams Specify a generation parameter.
     * @param text           Input text or prompt.
     * @return Inference generator.
     * @see Generator
     */
    public Generator generate(GenerateParameter generateParams, String text) {
        Preconditions.checkNotNull(generateParams, "Generate parameter cannot be null");
        Preconditions.checkNotNull(text, "Text cannot be null");
        if (generateParams.getLogitBias() != null && !generateParams.getLogitBias().isEmpty()) {
            generateParams.getLogitsProcessorList().add(new CustomBiasLogitsProcessor(generateParams.getLogitBias(), LlamaService.getVocabSize()));
        }
        if (generateParams.getStoppingWord() != null) {
            generateParams.getStoppingCriteriaList().add(new StoppingWordCriteria(generateParams.getStoppingWord()));
        }
        return new Generator(generateParams, text);
    }

    /**
     * Start a conversation and chat.
     *
     * @param question User question.
     * @return CompletionResult, generated text and completion reason.
     * @see CompletionResult
     */
    public CompletionResult chatCompletions(String question) {
        return chatCompletions(GenerateParameter.builder().build(), null, question);
    }

    /**
     * Start a conversation and chat.
     *
     * @param generateParams Specify a generation parameter.
     * @param question       User question.
     * @return CompletionResult, generated text and completion reason.
     * @see CompletionResult
     */
    public CompletionResult chatCompletions(GenerateParameter generateParams, String question) {
        return chatCompletions(generateParams, null, question);
    }

    /**
     * Start a conversation and chat.
     *
     * @param generateParams Specify a generation parameter.
     * @param system         System prompt.
     * @param question       User question.
     * @return CompletionResult, generated text and completion reason.
     * @see CompletionResult
     */
    public CompletionResult chatCompletions(GenerateParameter generateParams, String system, String question) {
        return chat(generateParams, system, question).result();
    }

    /**
     * Start a conversation and chat in streaming format.
     *
     * @param question User question.
     * @return Inference generator.
     * @see Generator
     */
    public Generator chat(String question) {
        return chat(GenerateParameter.builder().build(), null, question);
    }

    /**
     * Start a conversation and chat in streaming format.
     *
     * @param system   System prompt.
     * @param question User question.
     * @return Inference generator.
     * @see Generator
     */
    public Generator chat(String system, String question) {
        return chat(GenerateParameter.builder().build(), system, question);
    }

    /**
     * Start a conversation and chat in streaming format.
     *
     * @param generateParams Specify a generation parameter.
     * @param question       User question.
     * @return Inference generator.
     * @see Generator
     */
    public Generator chat(GenerateParameter generateParams, String question) {
        return chat(generateParams, null, question);
    }

    /**
     * Start a conversation and chat in streaming format.
     *
     * @param generateParams Specify a generation parameter.
     * @param system         System prompt.
     * @param question       User question.
     * @return Inference generator.
     * @see Generator
     */
    public Generator chat(GenerateParameter generateParams, String system, String question) {
        Preconditions.checkNotNull(generateParams, "Generate parameter cannot be null");
        Preconditions.checkNotNull(question, "Question cannot be null");
        Preconditions.checkNotNull(generateParams.getUser(), "User id cannot be null");
        if (generateParams.getLogitBias() != null && !generateParams.getLogitBias().isEmpty()) {
            generateParams.getLogitsProcessorList().add(new CustomBiasLogitsProcessor(generateParams.getLogitBias(), LlamaService.getVocabSize()));
        }
        if (generateParams.getStoppingWord() != null) {
            generateParams.getStoppingCriteriaList().add(new StoppingWordCriteria(generateParams.getStoppingWord()));
        }
        String key = StringUtils.isBlank(generateParams.getSession()) ? generateParams.getUser() : (generateParams.getUser() + ":" + generateParams.getSession());

        boolean exists = chatStatus.containsKey(key);
        if (!exists) {
            Status userStatus = new Status();
            chatStatus.put(key, userStatus);
            log.debug("Create new chat session, session: {} id: {}, chat session cache size: {}.", key, userStatus.getId(), chatStatus.size());
        }
        Status userStatus = chatStatus.get(key);
        if (StringUtils.isNotBlank(system) && system.equals(userStatus.getInitialSystemPrompt())) {
            system = null;
        }
        if (StringUtils.isNotBlank(system) && StringUtils.isBlank(userStatus.getInitialSystemPrompt())) {
            userStatus.setInitialSystemPrompt(system);
        }
        String prompt = PromptBuilder.format(ModelType.valueOf(modelType.toUpperCase()), system, question);
        return new Generator(generateParams, prompt, userStatus);
    }

    /**
     * Print generation metrics.
     * <p>Require verbose parameter to be true.</p>
     */
    public void metrics() {
        if (modelParams.isVerbose()) {
            log.info("Metrics: {}", LlamaService.getSamplingMetrics(true).toString());
        }
    }

    /**
     * Close the model and release resources.
     */
    @Override
    public void close() {
        removeAllChatStatus();
        LlamaService.release();
        LlamaService.llamaBackendFree();
        log.info("Closed model and context resources.");
    }

    @Override
    public String toString() {
        return "LlamaModel (" +
                "modelParams=" + modelParams +
                ')';
    }


}
