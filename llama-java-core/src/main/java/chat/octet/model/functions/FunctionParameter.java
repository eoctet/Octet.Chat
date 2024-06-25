package chat.octet.model.functions;

import chat.octet.model.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;


@Getter
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FunctionParameter {

    private String name;

    private String description;

    private boolean required;

    @Singular(value = "addSchema")
    private Map<String, String> schema;

    @Override
    public String toString() {
        return JsonUtils.toJson(this);
    }
}
