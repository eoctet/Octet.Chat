package chat.octet.model.beans;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class CompletionResult {
    private String content;
    private FinishReason finishReason;

}
