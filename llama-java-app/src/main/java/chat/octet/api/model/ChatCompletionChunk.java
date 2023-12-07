package chat.octet.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionChunk {
    private String id;
    private String model;
    private long created;
    private List<ChatCompletionData> choices = Lists.newArrayList();

    public ChatCompletionChunk() {
    }

    public ChatCompletionChunk(String id, String model, List<ChatCompletionData> choices) {
        this.id = id;
        this.model = model;
        this.created = System.currentTimeMillis();
        this.choices = choices;
    }

}
