package chat.octet.model.beans;


import chat.octet.model.LlamaService;
import chat.octet.model.enums.FinishReason;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;

import java.util.Arrays;
import java.util.List;

@Getter
@Slf4j
public class Status {
    private final int id;
    private final int contextSize;
    private final int[] inputIds;
    private List<Token> generateTokens;
    private int inputLength;
    private int pastTokenSize;
    @Setter
    private String systemPromptCache;

    public Status() {
        this.id = RandomUtils.nextInt(10000, 50000);
        this.contextSize = LlamaService.getContextSize();
        this.inputIds = new int[this.contextSize];
        this.generateTokens = Lists.newArrayList();
        this.inputLength = 0;
        this.pastTokenSize = 0;
    }

    public Status(Status srcStatus) {
        this.id = srcStatus.getId();
        this.contextSize = srcStatus.getContextSize();
        this.inputIds = new int[this.contextSize];
        this.generateTokens = Lists.newArrayList();
        if (srcStatus.getInputIds() != null && srcStatus.getInputLength() > 0) {
            System.arraycopy(srcStatus.getInputIds(), 0, this.inputIds, 0, srcStatus.getInputLength());
        }
        this.inputLength = srcStatus.getInputLength();
        this.pastTokenSize = srcStatus.getPastTokenSize();
        this.systemPromptCache = srcStatus.getSystemPromptCache();
    }

    public void appendTokens(int[] tokens) {
        if (inputLength + tokens.length >= contextSize) {
            reset();
            log.warn("Input tokens has exceeded the context size, status will be reset immediately, sequence id: {}.", id);
        }
        System.arraycopy(tokens, 0, inputIds, pastTokenSize, tokens.length);
        inputLength += tokens.length;
    }

    public void appendNextToken(Token token) {
        generateTokens.add(token);
        inputIds[inputLength] = token.getId();
        ++inputLength;
        ++pastTokenSize;
    }

    public void addPastTokensSize(int size) {
        pastTokenSize += size;
    }

    public void copyToStatus(Status srcStatus) {
        if (FinishReason.TRUNCATED == srcStatus.getFinishReason()) {
            reset();
        } else {
            System.arraycopy(srcStatus.getInputIds(), 0, inputIds, 0, srcStatus.getInputLength());
            inputLength = srcStatus.getInputLength();
            pastTokenSize = srcStatus.getPastTokenSize();
            generateTokens = Lists.newArrayList(srcStatus.getGenerateTokens());
            systemPromptCache = srcStatus.getSystemPromptCache();
        }
    }

    public FinishReason getFinishReason() {
        return (generateTokens == null || generateTokens.isEmpty()) ? FinishReason.UNKNOWN : generateTokens.get(generateTokens.size() - 1).getFinishReason();
    }

    public int[] subInputIds(int startIndexInclusive, int endIndexExclusive) {
        return ArrayUtils.subarray(inputIds, startIndexInclusive, endIndexExclusive);
    }

    public int[] subInputIds(int startIndexInclusive) {
        return subInputIds(startIndexInclusive, inputLength);
    }

    public void reset() {
        LlamaService.clearCache(id);
        Arrays.fill(inputIds, 0);
        pastTokenSize = 0;
        inputLength = 0;
        systemPromptCache = null;
    }

}
