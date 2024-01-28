package chat.octet.api.model;

import chat.octet.model.beans.ChatMessage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionRequestParameter {

    @JsonProperty("character")
    private String character;

    @JsonProperty("stream")
    private boolean stream;

    @JsonProperty("messages")
    private List<ChatMessage> messages;

    @JsonProperty("prompt")
    private String prompt;

}
