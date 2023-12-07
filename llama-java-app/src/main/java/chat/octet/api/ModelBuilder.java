package chat.octet.api;

import chat.octet.exceptions.ServerException;
import chat.octet.model.Model;
import chat.octet.model.parameters.ModelParameter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public final class ModelBuilder implements AutoCloseable {

    public static final String DEFAULT_MODEL_NAME = "Llama2-chat";
    public static final String MODEL_SETTINGS;
    private static volatile Model model;
    private static volatile ModelBuilder builder;

    static {
        MODEL_SETTINGS = StringUtils.join(Paths.get("").toAbsolutePath().toString(), File.separator, "conf", File.separator, "setting.json");
    }

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
                    ModelParameter modelParams = getModelSetting(name);
                    model = new Model(modelParams);
                }
            }
        }
        return model;
    }

    public Model getModel() {
        return getModel(DEFAULT_MODEL_NAME);
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

    public List<Pair<String, String>> getModelsList() {
        List<ModelParameter> modelParameters = getModelSettings();
        return modelParameters.stream().map(parameter -> Pair.of("id", parameter.getModelName())).collect(Collectors.toList());
    }

    public List<String> getModels() {
        List<ModelParameter> modelParameters = getModelSettings();
        return modelParameters.stream().map(ModelParameter::getModelName).collect(Collectors.toList());
    }

    private ModelParameter getModelSetting(String name) {
        List<ModelParameter> modelParameters = getModelSettings();
        for (ModelParameter parameter : modelParameters) {
            if (name.equalsIgnoreCase(parameter.getModelName())) {
                return parameter;
            }
        }
        throw new ServerException("Unable to find model settings, name: " + name);
    }

    private List<ModelParameter> getModelSettings() {
        File file = new File(MODEL_SETTINGS);
        if (!file.isFile() || !file.exists()) {
            throw new ServerException("Can not read model configuration file, please make sure it is valid");
        }
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            ObjectMapper mapper = new ObjectMapper();
            JavaType javaType = mapper.getTypeFactory().constructParametricType(List.class, ModelParameter.class);
            String json = bufferedReader.lines().collect(Collectors.joining());
            return mapper.readValue(json, javaType);
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
