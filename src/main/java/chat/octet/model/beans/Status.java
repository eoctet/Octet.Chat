package chat.octet.model.beans;


import chat.octet.model.LlamaService;
import com.google.common.collect.Lists;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;

@Getter
public class Status {

    private final int contextSize;
    private final int[] inputIds;
    private final List<Token> generateTokens;
    private int inputLength;
    private int pastTokensSize;

    public Status() {
        this.contextSize = LlamaService.getContextSize();
        this.inputIds = new int[contextSize];
        this.generateTokens = Lists.newArrayList();
        this.inputLength = 0;
        this.pastTokensSize = 0;
    }

    public Status(Status srcStatus) {
        this.contextSize = LlamaService.getContextSize();
        this.inputIds = new int[contextSize];
        this.generateTokens = Lists.newArrayList();
        if (srcStatus.getInputIds() != null && srcStatus.getInputLength() > 0) {
            System.arraycopy(srcStatus.getInputIds(), 0, this.inputIds, 0, srcStatus.getInputLength());
        }
        this.inputLength = srcStatus.getInputLength();
        this.pastTokensSize = srcStatus.getPastTokensSize();
    }

    public void appendInputIds(int[] tokens) {
        System.arraycopy(tokens, 0, this.inputIds, this.pastTokensSize, tokens.length);
        this.inputLength += tokens.length;
    }

    public void appendInputIds(Token token) {
        this.inputIds[this.inputLength] = token.getId();
        ++this.inputLength;
        generateTokens.add(token);
    }

    public boolean isOutOfContext() {
        return this.inputLength + 1 >= this.contextSize;
    }

    public void incrementPastTokensSize(int size) {
        this.pastTokensSize += size;
    }

    public void incrementPastTokensSize() {
        incrementPastTokensSize(1);
    }

    public int[] subInputIds(int startIndexInclusive, int endIndexExclusive) {
        return ArrayUtils.subarray(this.inputIds, startIndexInclusive, endIndexExclusive);
    }

    public int[] subInputIds(int startIndexInclusive) {
        return subInputIds(startIndexInclusive, this.inputLength);
    }

    public void copyToStatus(Status srcStatus) {
        System.arraycopy(srcStatus.getInputIds(), 0, this.inputIds, 0, srcStatus.getInputLength());
        this.inputLength = srcStatus.getInputLength();
        this.pastTokensSize = srcStatus.getPastTokensSize();
    }

    public int getLogitsIndex() {
        return (generateTokens.isEmpty() && pastTokensSize > 0) ? pastTokensSize - 1 : 0;
    }

    public String getGeneratedCompleteText() {
        StringBuilder builder = new StringBuilder();
        generateTokens.forEach(token -> builder.append(token.getText()));
        return builder.toString();
    }

    public void reset() {
        Arrays.fill(this.inputIds, 0);
        this.pastTokensSize = 0;
        this.inputLength = 0;
    }

}
