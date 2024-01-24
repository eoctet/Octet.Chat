package chat.octet.api;

import chat.octet.config.ModelConfig;
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
public final class ModelBuilder implements AutoCloseable {

    public static final String DEFAULT_MODEL_NAME = "Llama2-chat";
    private static volatile Model model;
    private static volatile ModelBuilder builder;

    private ModelConfig modelConfig;

    private ModelBuilder() {
    }

    public static ModelBuilder getInstance() {
        if (builder == null) {
            synchronized (ModelBuilder.class) {
                if (builder == null) {
                    builder = new ModelBuilder();
                }
            }
        }
        return builder;
    }

    public Model getModel(String name) {
        if (model == null) {
            synchronized (ModelBuilder.class) {
                if (model == null) {
                    modelConfig = getModelConfig(name);
                    model = new Model(modelConfig.getModelParameter());
                }
            }
        }
        return model;
    }

    public Model reloadModel(String name) {
        synchronized (ModelBuilder.class) {
            if (model != null) {
                model.close();
                model = null;
            }
        }
        return getModel(name);
    }

    private ModelConfig getModelConfig(String name) {
        String filePath = StringUtils.join(Paths.get("").toAbsolutePath().toString(), File.separator, "conf", File.separator, name, ".json");
        File file = new File(filePath);
        if (!file.isFile() || !file.exists()) {
            throw new ServerException("Can not read model configuration file, please make sure it is valid");
        }
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            ObjectMapper mapper = new ObjectMapper();
            String json = bufferedReader.lines().collect(Collectors.joining());
            return mapper.readValue(json, ModelConfig.class);
        } catch (Exception e) {
            throw new ServerException("Parse model configuration file error", e);
        }
    }

    @Override
    public void close() {
        if (model != null) {
            model.close();
        }
    }
}
