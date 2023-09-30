package chat.octet.model.beans;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Chat message entity
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
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

    /**
     * Chat role define
     *
     * @author <a href="https://github.com/eoctet">William</a>
     */
    public enum ChatRole {
        /**
         * System prompt
         */
        SYSTEM,
        /**
         * User role
         */
        USER,
        /**
         * Assistant role
         */
        ASSISTANT
    }

    public static ChatMessage toSystem(String content) {
        return new ChatMessage(ChatRole.SYSTEM, content);
    }

    public static ChatMessage toUser(String content) {
        return new ChatMessage(ChatRole.USER, content);
    }

    public static ChatMessage toAssistant(String content) {
        return new ChatMessage(ChatRole.ASSISTANT, content);
    }
}
