package chat.octet.config;


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

    private String name;
    private String prompt;
    private ModelParameter modelParameter = ModelParameter.builder().build();
    private GenerateParameter generateParameter = GenerateParameter.builder().build();

}
