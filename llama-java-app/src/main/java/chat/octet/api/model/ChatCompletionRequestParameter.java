package chat.octet.api.model;

import chat.octet.model.beans.ChatMessage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionRequestParameter {

    @JsonProperty("model")
    private String modelName;

    @JsonProperty("stream")
    private boolean stream;

    @JsonProperty("messages")
    private ChatMessage messages;

    @JsonProperty("prompt")
    private String prompt;

}
