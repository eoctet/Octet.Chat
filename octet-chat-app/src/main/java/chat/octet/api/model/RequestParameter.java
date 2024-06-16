package chat.octet.api.model;

import chat.octet.model.beans.ChatMessage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RequestParameter {

    //chat & completions parameters
    private String character;
    private String model;
    private boolean stream;
    private String user;
    private String session;

    //chat parameters
    private List<ChatMessage> messages;

    //completions parameters
    private String prompt;
    private boolean echo;

    //tokenize & detokenize parameters
    private String content;
    private int[] tokens;

}
