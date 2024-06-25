package chat.octet.api.functions.model;

import chat.octet.model.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FunctionConfig {
    private String name;
    private String alias;
    private String description;
    private RequestConfig config;
    private List<Parameter> inputParameters;
    private List<Parameter> outputParameters;

    @Override
    public String toString() {
        return JsonUtils.toJson(this);
    }
}
