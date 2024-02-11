package chat.octet.model.beans;

import chat.octet.model.enums.FinishReason;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Completion result entity
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Getter
@Builder
@ToString
public class CompletionResult {
    private String content;
    private int promptTokens;
    private int completionTokens;
    private FinishReason finishReason;

}
