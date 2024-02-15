package chat.octet.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CharacterModel {
    private String name;
    private String modelName;
    private String modelType;

    public CharacterModel(String name, String modelName, String modelType) {
        this.name = name;
        this.modelName = modelName;
        this.modelType = modelType;
    }

    public CharacterModel() {
    }
}
