package chat.octet.agent.plugin.model;


import chat.octet.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;
import java.util.List;

@Getter
@Builder
@ToString
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PluginConfig {
    private String pluginType;
    private String nameForHuman;
    private String nameForModel;
    private String descriptionForModel;
    private List<Parameter> inputParameters;
    private List<Parameter> outputParameters;
    private Object config;

    public <T> T getConfig(Class<T> clazz) {
        if (config instanceof LinkedHashMap) {
            config = JsonUtils.parseToObject(JsonUtils.toJson(config), clazz);
        }
        if (config != null && config.getClass() == clazz) {
            return clazz.cast(config);
        }
        return null;
    }

    public <T> T getConfig(Class<T> clazz, String errorMessage) {
        T object = getConfig(clazz);
        if (object == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        return object;
    }

}
