package chat.octet.api;

import chat.octet.agent.OctetAgent;
import chat.octet.agent.plugin.PluginManager;
import chat.octet.config.CharacterConfig;
import chat.octet.exceptions.ServerException;
import chat.octet.model.Model;
import chat.octet.model.enums.ModelType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public final class CharacterModelBuilder implements AutoCloseable {
    public static final String DEFAULT_CHARACTER_NAME = "llama2-chat";
    private static volatile Model model;
    private static volatile CharacterModelBuilder builder;

    private CharacterConfig defaultCharacterConfig;

    private CharacterModelBuilder() {
    }

    public static CharacterModelBuilder getInstance() {
        if (builder == null) {
            synchronized (CharacterModelBuilder.class) {
                if (builder == null) {
                    builder = new CharacterModelBuilder();
                }
            }
        }
        return builder;
    }

    private static CharacterConfig getCharacterConfig(File file) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            ObjectMapper mapper = new ObjectMapper();
            String json = bufferedReader.lines().collect(Collectors.joining());
            return mapper.readValue(json, CharacterConfig.class);
        } catch (Exception e) {
            throw new ServerException("Parse characters configuration file error: " + file.getName(), e);
        }
    }

    public Model getCharacterModel() {
        if (model == null) {
            throw new ServerException("No available models, please reload the model.");
        }
        return model;
    }

    public Model getCharacterModel(String characterName) {
        if (model == null) {
            synchronized (CharacterModelBuilder.class) {
                if (model == null) {
                    Map<String, CharacterConfig> characterConfigs = getCharacterConfigs();
                    if (!characterConfigs.containsKey(characterName)) {
                        throw new ServerException("No available character config, please check if the character name is available: " + characterName);
                    }
                    defaultCharacterConfig = characterConfigs.get(characterName);
                    model = new Model(defaultCharacterConfig.getModelParameter());

                    if (defaultCharacterConfig.isAgentMode()) {
                        if (ModelType.QWEN != ModelType.valueOf(model.getModelType())) {
                            throw new IllegalArgumentException("AI Agent only supports Qwen series model");
                        }
                        PluginManager.getInstance().loadPlugins();
                        OctetAgent.getInstance().reset();
                    }
                }
            }
        }
        return model;
    }

    public void reloadCharacterModel(String characterName) {
        synchronized (CharacterModelBuilder.class) {
            if (model != null) {
                model.close();
                model = null;
            }
        }
        getCharacterModel(characterName);
    }

    public CharacterConfig getCharacterConfig() {
        return defaultCharacterConfig;
    }

    public Map<String, CharacterConfig> getCharacterConfigs() {
        Map<String, CharacterConfig> characterConfigs = Maps.newLinkedHashMap();
        String filePath = StringUtils.join(Paths.get("").toAbsolutePath().toString(), File.separator, "characters", File.separator);
        Path dir = FileSystems.getDefault().getPath(filePath);
        try (Stream<Path> paths = Files.list(dir)) {
            paths.forEach(path -> {
                try {
                    File file = path.toFile();
                    if (file.getName().endsWith(".json") && !file.getName().equalsIgnoreCase("plugins.json")) {
                        CharacterConfig config = getCharacterConfig(path.toFile());
                        if (config != null) {
                            characterConfigs.put(config.getName(), config);
                        }
                    }
                } catch (Exception e) {
                    log.error("", e);
                }
            });
        } catch (Exception e) {
            log.error("Query file list error ", e);
        }
        return characterConfigs;
    }

    @Override
    public void close() {
        if (model != null) {
            model.close();
        }
    }
}
