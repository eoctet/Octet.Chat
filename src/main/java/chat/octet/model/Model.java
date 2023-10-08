package chat.octet.model;


import chat.octet.model.beans.*;
import chat.octet.model.exceptions.ModelException;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.parameters.ModelParameter;
import chat.octet.model.utils.PromptBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

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
    private final int lastTokensSize;
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
        this.lastTokensSize = modelParams.getLastNTokensSize() < 0 ? LlamaService.getContextSize() : modelParams.getLastNTokensSize();
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
        llamaContextParams.ropeFreqBase = modelParams.getRopeFreqBase();
        llamaContextParams.ropeFreqScale = modelParams.getRopeFreqScale();
        llamaContextParams.mulMatQ = modelParams.isMulMatQ();
        llamaContextParams.f16KV = modelParams.isF16KV();
        llamaContextParams.logitsAll = modelParams.isLogitsAll();
        llamaContextParams.embedding = modelParams.isEmbedding();
        return llamaContextParams;
    }

    public void removeChatStatus(String user) {
        boolean exists = chatStatus.containsKey(user);
        if (exists) {
            Status status = chatStatus.remove(user);
            if (status != null) {
                status.reset();
            }
            log.info("Removed chat session, User: {}.", user);
        }
    }

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
        Iterable<Token> tokenIterable = generate(generateParams, text);
        tokenIterable.forEach(e -> {
        });
        Generator generator = (Generator) tokenIterable.iterator();
        return CompletionResult.builder().content(generator.getGeneratedCompleteText()).finishReason(generator.getFinishReason()).build();
    }

    /**
     * Generate text in stream format.
     *
     * @param text Input text or prompt.
     * @return Iterable, Generation iterator.
     * @see Generator
     */
    public Iterable<Token> generate(String text) {
        return generate(GenerateParameter.builder().build(), text);
    }

    /**
     * Generate text in stream format.
     *
     * @param generateParams Specify a generation parameter.
     * @param text           Input text or prompt.
     * @return Iterable, Generation iterator.
     * @see Generator
     */
    public Iterable<Token> generate(GenerateParameter generateParams, String text) {
        Preconditions.checkNotNull(generateParams, "Generate parameter cannot be null");
        Preconditions.checkNotNull(text, "Text cannot be null");
        generateParams.setLastTokensSize(lastTokensSize);

        return new Iterable<Token>() {

            private Generator generator;

            @Nonnull
            @Override
            public Iterator<Token> iterator() {
                if (generator == null) {
                    generator = new Generator(generateParams, text);
                }
                return generator;
            }

            @Override
            public void forEach(Consumer<? super Token> action) {
                Objects.requireNonNull(action);
                try {
                    for (Token token : this) {
                        action.accept(token);
                    }
                } catch (Exception e) {
                    throw new ModelException("Generate next token error ", e);
                } finally {
                    generator.clearCache();
                }
            }
        };
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
        Iterable<Token> tokenIterable = chat(generateParams, system, question);
        tokenIterable.forEach(e -> {
        });
        Generator generator = (Generator) tokenIterable.iterator();
        return CompletionResult.builder().content(generator.getGeneratedCompleteText()).finishReason(generator.getFinishReason()).build();
    }

    /**
     * Start a conversation and chat in streaming format.
     *
     * @param question User question.
     * @return Iterable, Generation iterator.
     * @see Generator
     */
    public Iterable<Token> chat(String question) {
        return chat(GenerateParameter.builder().build(), null, question);
    }

    /**
     * Start a conversation and chat in streaming format.
     *
     * @param system   System prompt.
     * @param question User question.
     * @return Iterable, Generation iterator.
     * @see Generator
     */
    public Iterable<Token> chat(String system, String question) {
        return chat(GenerateParameter.builder().build(), system, question);
    }

    /**
     * Start a conversation and chat in streaming format.
     *
     * @param generateParams Specify a generation parameter.
     * @param question       User question.
     * @return Iterable, Generation iterator.
     * @see Generator
     */
    public Iterable<Token> chat(GenerateParameter generateParams, String question) {
        return chat(generateParams, null, question);
    }

    /**
     * Start a conversation and chat in streaming format.
     *
     * @param generateParams Specify a generation parameter.
     * @param system         System prompt.
     * @param question       User question.
     * @return Iterable, Generation iterator.
     * @see Generator
     */
    public Iterable<Token> chat(GenerateParameter generateParams, String system, String question) {
        Preconditions.checkNotNull(generateParams, "Generate parameter cannot be null");
        Preconditions.checkNotNull(question, "Question cannot be null");
        Preconditions.checkNotNull(generateParams.getUser(), "User id cannot be null");
        generateParams.setLastTokensSize(lastTokensSize);

        boolean exists = chatStatus.containsKey(generateParams.getUser());
        if (!exists) {
            Status userStatus = new Status();
            chatStatus.put(generateParams.getUser(), userStatus);
            log.debug("Create new chat session, User: {} id: {}, chat session cache size: {}.", generateParams.getUser(), userStatus.getId(), chatStatus.size());
        }
        Status userStatus = chatStatus.get(generateParams.getUser());
        if (StringUtils.isNotBlank(system) && system.equals(userStatus.getInitialSystemPrompt())) {
            system = null;
        }
        if (StringUtils.isNotBlank(system) && StringUtils.isBlank(userStatus.getInitialSystemPrompt())) {
            userStatus.setInitialSystemPrompt(system);
        }
        String prompt = PromptBuilder.toPrompt(system, question);
        return new Iterable<Token>() {
            private Generator generator;
            private Status userChatStatus;

            @Nonnull
            @Override
            public Iterator<Token> iterator() {
                if (generator == null) {
                    generator = new Generator(generateParams, prompt, userStatus);
                }
                if (userChatStatus == null) {
                    userChatStatus = userStatus;
                }
                return generator;
            }

            @Override
            public void forEach(Consumer<? super Token> action) {
                Objects.requireNonNull(action);
                try {
                    for (Token token : this) {
                        action.accept(token);
                    }
                } catch (Exception e) {
                    throw new ModelException("Generate next token error ", e);
                } finally {
                    //copy last generated status
                    userChatStatus.copyToStatus(generator.getStatus());
                }
            }
        };
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
    }

    @Override
    public String toString() {
        return "LlamaModel (" +
                "modelParams=" + modelParams +
                ')';
    }


}
