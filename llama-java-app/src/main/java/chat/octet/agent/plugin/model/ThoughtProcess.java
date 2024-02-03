package chat.octet.agent.plugin.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ThoughtProcess {
    private String thought;
    private String action;
    private String actionInput;
    private String observation;
    private String finalAnswer;

    @JsonIgnore
    public boolean isComplete() {
        return !StringUtils.isAnyBlank(thought, action, actionInput);
    }

}
