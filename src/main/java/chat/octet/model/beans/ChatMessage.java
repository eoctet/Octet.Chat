package chat.octet.model.beans;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    private ChatRole role;
    private String content;

    public ChatMessage() {
    }

    public ChatMessage(ChatRole role, String content) {
        this.role = role;
        this.content = content;
    }

    public enum ChatRole {
        SYSTEM, USER, ASSISTANT
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(ChatRole.SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(ChatRole.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(ChatRole.ASSISTANT, content);
    }
}
