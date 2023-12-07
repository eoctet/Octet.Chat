package chat.octet.api.model;

import chat.octet.model.beans.ChatMessage;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionData {
    private int index;
    private String text;
    private Pair<String, String> delta;
    private ChatMessage message;
    @JsonProperty("finish_reason")
    private String finishReason;

    public ChatCompletionData() {
    }

    public ChatCompletionData(String key, String value, String finishReason) {
        this.index = 0;
        this.delta = Pair.of(key, value);
        this.finishReason = finishReason;
    }

    public ChatCompletionData(ChatMessage message, String finishReason) {
        this.index = 0;
        this.message = message;
        this.finishReason = finishReason;
    }

    public ChatCompletionData(String text, String finishReason) {
        this.index = 0;
        this.text = text;
        this.finishReason = finishReason;
    }
}
