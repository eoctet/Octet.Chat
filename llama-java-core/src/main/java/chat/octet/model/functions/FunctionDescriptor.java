package chat.octet.model.functions;

import chat.octet.model.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;

import java.util.List;


@Getter
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FunctionDescriptor {

    @Builder.Default
    private String type = FunctionConstants.FUNCTION_DEFAULT_TYPE;

    private String name;

    private String alias;

    private String description;

    @Singular(value = "addParameter")
    private List<FunctionParameter> parameters;

    @Override
    public String toString() {
        return JsonUtils.toJson(this);
    }
}
