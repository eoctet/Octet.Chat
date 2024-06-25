package chat.octet.model;


import chat.octet.model.beans.*;
import chat.octet.model.components.criteria.impl.StoppingWordCriteria;
import chat.octet.model.components.processor.impl.CustomBiasLogitsProcessor;
import chat.octet.model.components.prompt.ChatTemplateFormatter;
import chat.octet.model.components.prompt.DefaultChatTemplateFormatter;
import chat.octet.model.exceptions.ModelException;
import chat.octet.model.functions.Function;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.parameters.ModelParameter;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private final ChatTemplateFormatter chatFormatter;
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

        //Load model and initialize
        LlamaModelParams llamaModelParams = getLlamaModelParameters(modelParams);
        LlamaContextParams llamaContextParams = getLlamaContextParameters(modelParams);
        LlamaService.loadLlamaModelFromFile(modelParams.getModelPath(), llamaModelParams, llamaContextParams);

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
        //load model meta
        this.modelName = LlamaService.llamaModelMeta("general.name");
        this.modelType = LlamaService.llamaModelMeta("general.architecture");

        //load chat template, by default use the template from local resource.
        String defaultChatTemplate = LlamaService.llamaModelMeta("tokenizer.chat_template");
        this.chatFormatter = Optional.ofNullable(modelParams.getChatTemplateFormatter())
                .orElse(new DefaultChatTemplateFormatter(this.modelType, defaultChatTemplate));

        log.info(LlamaService.getSystemInfo());
        log.info(this.toString());
        log.info("Model loaded successfully.");
    }

    private LlamaModelParams getLlamaModelParameters(ModelParameter modelParams) {
        LlamaModelParams llamaModelParams = LlamaService.getLlamaModelDefaultParams();
        llamaModelParams.gpuLayers = modelParams.getGpuLayers();
        llamaModelParams.splitMode = modelParams.getSplitMode();
        llamaModelParams.mainGpu = modelParams.getMainGpu();
        if (modelParams.getTensorSplit() != null) {
            llamaModelParams.tensorSplit = modelParams.getTensorSplit();
        }
        llamaModelParams.vocabOnly = modelParams.isVocabOnly();
        boolean mmap = (StringUtils.isBlank(modelParams.getLoraPath()) && modelParams.isMmap());
        if (mmap && LlamaService.isMmapSupported()) {
            llamaModelParams.mmap = true;
        }
        boolean mlock = modelParams.isMlock();
        if (mlock && LlamaService.isMlockSupported()) {
            llamaModelParams.mlock = true;
        }
        llamaModelParams.checkTensors = modelParams.isCheckTensors();
        llamaModelParams.numaStrategy = modelParams.getNumaStrategy();
        return llamaModelParams;
    }

    private LlamaContextParams getLlamaContextParameters(ModelParameter modelParams) {
        LlamaContextParams llamaContextParams = LlamaService.getLlamaContextDefaultParams();
        llamaContextParams.seed = modelParams.getSeed();
        llamaContextParams.ctx = modelParams.getContextSize();
        llamaContextParams.batch = modelParams.getBatchSize();
        llamaContextParams.ubatch = modelParams.getUbatch();
        llamaContextParams.seqMax = modelParams.getSeqMax();
        llamaContextParams.threads = modelParams.getThreads();
        llamaContextParams.threadsBatch = modelParams.getThreadsBatch() == -1 ? modelParams.getThreads() : modelParams.getThreadsBatch();
        llamaContextParams.ropeScalingType = modelParams.getRopeScalingType();
        llamaContextParams.poolingType = modelParams.getPoolingType();
        llamaContextParams.yarnExtFactor = modelParams.getYarnExtFactor();
        llamaContextParams.yarnAttnFactor = modelParams.getYarnAttnFactor();
        llamaContextParams.yarnBetaFast = modelParams.getYarnBetaFast();
        llamaContextParams.yarnBetaSlow = modelParams.getYarnBetaSlow();
        llamaContextParams.yarnOrigCtx = modelParams.getYarnOrigCtx();
        llamaContextParams.defragThold = modelParams.getDefragThold();
        llamaContextParams.ropeFreqBase = modelParams.getRopeFreqBase();
        llamaContextParams.ropeFreqScale = modelParams.getRopeFreqScale();
        llamaContextParams.logitsAll = modelParams.isLogitsAll();
        llamaContextParams.embedding = modelParams.isEmbedding();
        llamaContextParams.offloadKqv = modelParams.isOffloadKqv();
        llamaContextParams.flashAttn = modelParams.isFlashAttn();
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
     * @param system         System prompt.
     * @param question       User question.
     * @return Inference generator.
     * @see Generator
     */
    public Generator chat(GenerateParameter generateParams, String system, String question) {
        Preconditions.checkNotNull(question, "User question cannot be null");
        if (StringUtils.isNotBlank(system)) {
            return chat(generateParams, Lists.newArrayList(ChatMessage.toSystem(system), ChatMessage.toUser(question)));
        } else {
            return chat(generateParams, Lists.newArrayList(ChatMessage.toUser(question)));
        }
    }

    /**
     * Start a conversation and chat in streaming format.
     *
     * @param generateParams Specify a generation parameter.
     * @param messages       Chat messages.
     * @return Inference generator.
     * @see Generator
     */
    public Generator chat(GenerateParameter generateParams, List<ChatMessage> messages) {
        return chat(generateParams, messages, null, null);
    }

    public Generator chat(GenerateParameter generateParams, List<ChatMessage> messages, List<Function> functions) {
        return chat(generateParams, messages, functions, null);
    }

    /**
     * Start a conversation and chat in streaming format.
     *
     * @param generateParams Specify a generation parameter.
     * @param messages       Chat messages list.
     * @param functions      Function list.
     * @param params         Chat template parameters.
     * @return Inference generator.
     * @see Generator
     */
    public Generator chat(GenerateParameter generateParams, List<ChatMessage> messages, List<Function> functions, Map<String, Object> params) {
        Preconditions.checkNotNull(generateParams, "Generate parameter cannot be null");
        Preconditions.checkNotNull(generateParams, "Chat messages cannot be null");
        if (messages.size() == 1 && ChatMessage.ChatRole.SYSTEM == messages.get(0).getRole()) {
            throw new IllegalArgumentException("Chat messages cannot be only one system message");
        }

        //add custom logit bias
        if (generateParams.getLogitBias() != null && !generateParams.getLogitBias().isEmpty()) {
            generateParams.getLogitsProcessorList().add(new CustomBiasLogitsProcessor(generateParams.getLogitBias(), LlamaService.getVocabSize()));
        }
        //add custom stopping word
        if (generateParams.getStoppingWord() != null) {
            generateParams.getStoppingCriteriaList().add(new StoppingWordCriteria(generateParams.getStoppingWord()));
        }

        Status status = null;
        //if session cache is enabled, try to retrieve the chat session from the cache
        //otherwise does not use session cache in chat
        if (generateParams.isSessionCache()) {
            Preconditions.checkNotNull(generateParams.getUser(), "Chat user cannot be null, please set user in generate parameter.");
            String key = StringUtils.isBlank(generateParams.getSession()) ? generateParams.getUser() : (generateParams.getUser() + ":" + generateParams.getSession());
            boolean exists = chatStatus.containsKey(key);
            if (!exists) {
                status = new Status();
                chatStatus.put(key, status);
                log.debug("Create new chat session, session: {} id: {}, chat session cache size: {}.", key, status.getId(), chatStatus.size());
            }
            status = chatStatus.get(key);

            //if prompt cache is enabled, set the initial system prompt and does not update it again
            if (generateParams.isPromptCache()) {
                ChatMessage msg = messages.stream().filter(m -> ChatMessage.ChatRole.SYSTEM == m.getRole()).findFirst().orElse(null);
                if (msg != null && StringUtils.isNotBlank(msg.getContent())) {
                    if (!msg.getContent().equals(status.getSystemPromptCache())) {
                        status.setSystemPromptCache(msg.getContent());
                    } else {
                        //remove the system prompt in messages
                        messages.remove(msg);
                    }
                }
            }
        }

        String prompt = chatFormatter.format(messages, functions, true, params);
        return new Generator(generateParams, prompt, status);
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
        return "model name: " + modelName +
                ", model type: " + modelType +
                ", model parameters: " + modelParams;
    }


}
