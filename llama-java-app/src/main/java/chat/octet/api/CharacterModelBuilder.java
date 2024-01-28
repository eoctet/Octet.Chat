package chat.octet.api;

import chat.octet.config.CharacterConfig;
import chat.octet.exceptions.ServerException;
import chat.octet.model.Model;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@Getter
@Slf4j
public final class CharacterModelBuilder implements AutoCloseable {

    private static volatile Model model;
    private static volatile CharacterModelBuilder builder;

    private CharacterConfig characterConfig;

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

    public Model getCharacterModel(String characterName) {
        if (model == null) {
            synchronized (CharacterModelBuilder.class) {
                if (model == null) {
                    characterConfig = getCharacterConfig(characterName);
                    model = new Model(characterConfig.getModelParameter());
                }
            }
        }
        return model;
    }

    public Model reloadCharacterModel(String characterName) {
        synchronized (CharacterModelBuilder.class) {
            if (model != null) {
                model.close();
                model = null;
            }
        }
        return getCharacterModel(characterName);
    }

    private CharacterConfig getCharacterConfig(String characterName) {
        String filePath = StringUtils.join(Paths.get("").toAbsolutePath().toString(), File.separator, "characters", File.separator, characterName, ".json");
        File file = new File(filePath);
        if (!file.isFile() || !file.exists()) {
            throw new ServerException("Can not read character configuration file, please make sure it is valid");
        }
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            ObjectMapper mapper = new ObjectMapper();
            String json = bufferedReader.lines().collect(Collectors.joining());
            return mapper.readValue(json, CharacterConfig.class);
        } catch (Exception e) {
            throw new ServerException("Parse characters configuration file error", e);
        }
    }

    @Override
    public void close() {
        if (model != null) {
            model.close();
        }
    }
}
