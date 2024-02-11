package chat.octet.api.model;

import chat.octet.model.beans.ChatMessage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionRequestParameter {

    //chat & completions parameters
    @JsonProperty("character")
    private String character;

    @JsonProperty("model")
    private String model;

    @JsonProperty("stream")
    private boolean stream;

    @JsonProperty("user")
    private String user;

    @JsonProperty("session")
    private String session;

    //chat parameters
    @JsonProperty("messages")
    private List<ChatMessage> messages;

    //completions parameters
    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("echo")
    private boolean echo;

    //tokenize & detokenize parameters
    @JsonProperty("content")
    private String content;

    @JsonProperty("tokens")
    private int[] tokens;

}
