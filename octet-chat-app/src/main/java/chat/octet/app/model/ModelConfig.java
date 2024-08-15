package chat.octet.app.model;


import chat.octet.model.parameters.GenerateParameter;
import chat.octet.model.parameters.ModelParameter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ModelConfig {

    private ModelParameter modelParameter;
    private GenerateParameter generateParameter;

    public static ModelConfig getDefault() {
        ModelConfig config = new ModelConfig();
        config.setModelParameter(ModelParameter.builder().build());
        config.setGenerateParameter(GenerateParameter.builder().build());
        return config;
    }

}
