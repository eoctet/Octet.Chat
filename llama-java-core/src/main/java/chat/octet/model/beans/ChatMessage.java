package chat.octet.model.beans;

import chat.octet.model.functions.FunctionCall;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Chat message entity
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChatMessage {
    @JsonIgnore
    private String id;
    private ChatRole role;
    private String content;
    private List<FunctionCall> toolCalls;

    public ChatMessage() {
    }

    public ChatMessage(ChatRole role, String content) {
        this.id = UUID.randomUUID().toString().toLowerCase();
        this.role = role;
        this.content = content;
    }

    public ChatMessage(ChatRole role, String content, List<FunctionCall> toolCalls) {
        this.id = UUID.randomUUID().toString().toLowerCase();
        this.role = role;
        this.content = content;
        this.toolCalls = toolCalls;
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

    public static ChatMessage toFunction(String content) {
        return new ChatMessage(ChatRole.FUNCTION, content);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatMessage that)) return false;
        return Objects.equals(id, that.id) && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, role);
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
        ASSISTANT,
        /**
         * Function role
         */
        FUNCTION;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
