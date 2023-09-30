package chat.octet.model.beans;

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
    private FinishReason finishReason;

}
