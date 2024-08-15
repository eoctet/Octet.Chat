package chat.octet.app.service;

import chat.octet.app.core.constants.AppConstants;
import chat.octet.app.core.exceptions.AppException;
import chat.octet.app.core.exceptions.ResourceException;
import chat.octet.app.model.CharacterConfig;
import chat.octet.app.utils.FileUtils;
import chat.octet.model.Generator;
import chat.octet.model.Model;
import chat.octet.model.beans.ChatMessage;
import chat.octet.model.beans.Token;
import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.parameters.ModelParameter;
import chat.octet.model.utils.JsonUtils;
import com.google.common.collect.Maps;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

@Slf4j
public final class ChatService implements AutoCloseable {
    private static volatile ChatService handler;
    private final Semaphore semaphore = new Semaphore(1, true);

    @Getter
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    @Getter
    private CharacterConfig characterConfig;
    private Model model;
    private ChatHandler service;

    private ChatService() {
    }

    public static ChatService get() {
        if (handler == null) {
            synchronized (ChatService.class) {
                if (handler == null) {
                    handler = new ChatService();
                }
            }
        }
        return handler;
    }

    public boolean isRunning() {
        return semaphore.availablePermits() == 0;
    }

    public void loadCharacter(String characterId) throws ResourceException {
        if (!semaphore.tryAcquire()) {
            throw new ResourceException("Too many requests, please wait for a while.");
        }
        try {
            if (isModelLoaded()) {
                close();
            }
            Map<String, CharacterConfig> configs = getCharacterConfigList();
            if (!configs.containsKey(characterId)) {
                throw new AppException(String.format("No available character config in %s, please check if the character name is available: %s", AppConstants.APP_CHARACTERS_CONFIG_PATH, characterId));
            }
            characterConfig = configs.get(characterId);
            ModelParameter modelParameter = characterConfig.getModelConfig().getModelParameter();
            modelParameter.setModelPath(characterConfig.getModelPath());
            model = new Model(modelParameter);
        } finally {
            if (semaphore.availablePermits() == 0) {
                semaphore.release();
            }
        }
    }

    public boolean isModelLoaded() {
        return model != null && !model.isClosed();
    }

    public Map<String, CharacterConfig> getCharacterConfigList() {
        Map<String, CharacterConfig> configs = Maps.newLinkedHashMap();
        Path dir = FileSystems.getDefault().getPath(AppConstants.APP_CHARACTERS_CONFIG_PATH);
        try (Stream<Path> paths = Files.list(dir)) {
            paths.forEach(path -> {
                try {
                    File file = path.toFile();
                    if (file.getName().endsWith(".json")) {
                        String json = FileUtils.readFile(file.getAbsolutePath());
                        CharacterConfig config = JsonUtils.parseToObject(json, CharacterConfig.class);
                        if (config != null) {
                            configs.put(config.getId(), config);
                        }
                    }
                } catch (IOException e) {
                    log.error("", e);
                }
            });
        } catch (Exception e) {
            log.error("Parse characters config error", e);
        }
        return configs;
    }

    public ChatHandler newChatHandler(String sessionId, List<ChatMessage> messages) throws ResourceException {
        if (!isModelLoaded()) {
            throw new AppException("Model not loaded, please load the model first.");
        }
        if (!semaphore.tryAcquire()) {
            throw new ResourceException("Too many requests, please wait for a while.");
        }
        running.set(true);
        GenerateParameter generateParams = characterConfig.getModelConfig().getGenerateParameter();
        generateParams.setSession(sessionId);
        service = new ChatHandler(model, generateParams, messages, semaphore, running);
        return service;
    }

    public void removeChatSession(String sessionId) {
        if (isModelLoaded()) {
            model.removeChatStatus(sessionId);
        }
    }

    @Override
    public void close() {
        if (service != null && service.isRunning()) {
            service.cancel();
        }
        if (model != null) {
            model.close();
            model = null;
        }
    }

    @Slf4j
    public static class ChatHandler extends Service<Void> {
        private final Model model;
        private final GenerateParameter generateParameter;
        private final List<ChatMessage> messages;
        private final Semaphore semaphore;
        private final BooleanProperty running;

        public ChatHandler(Model model, GenerateParameter generateParameter, List<ChatMessage> messages, Semaphore semaphore, BooleanProperty running) {
            this.model = model;
            this.generateParameter = generateParameter;
            this.messages = messages;
            this.semaphore = semaphore;
            this.running = running;
        }

        private void releaseLock() {
            semaphore.release();
            running.set(false);
            log.debug("Release lock, session id: {}", generateParameter.getSession());
        }

        @Override
        protected void succeeded() {
            try {
                super.succeeded();
            } finally {
                releaseLock();
            }
        }

        @Override
        protected void failed() {
            try {
                super.failed();
            } finally {
                releaseLock();
            }
        }

        @Override
        public boolean cancel() {
            try {
                return super.cancel();
            } finally {
                releaseLock();
            }
        }

        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() {
                    Generator generator = model.chat(generateParameter, messages);
                    try {
                        StringBuilder buffer = new StringBuilder();
                        for (Token token : generator) {
                            buffer.append(token.getText());
                            updateMessage(buffer.toString());
                            if (isCancelled()) {
                                log.debug("Chat stopped by user, session id: {}", generateParameter.getSession());
                                break;
                            }
                        }
                    } finally {
                        generator.close();
                        model.metrics();
                    }
                    return null;
                }
            };
        }

    }

}
