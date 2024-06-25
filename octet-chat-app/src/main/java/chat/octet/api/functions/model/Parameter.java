package chat.octet.api.functions.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Parameter {
    private String name;
    private String description;
    private boolean required;
    private String type;

    public Parameter(String name, String description, boolean required, String type) {
        this.name = name;
        this.description = description;
        this.required = required;
        this.type = type;
    }

    public Parameter(String name, String description, String type) {
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public Parameter() {
    }
}
