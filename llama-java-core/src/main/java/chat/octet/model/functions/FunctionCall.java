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
import java.util.UUID;

@Getter
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FunctionCall {

    @Builder.Default
    private String id = UUID.randomUUID().toString().toLowerCase();

    @Builder.Default
    private String type = FunctionConstants.FUNCTION_DEFAULT_TYPE;

    @Singular(value = "addParameter")
    private Map<String, Object> function;

    @Override
    public String toString() {
        return JsonUtils.toJson(this);
    }
}
