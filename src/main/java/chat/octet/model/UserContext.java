package chat.octet.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;


public class UserContext implements Serializable {

    @Getter
    private final String id;
    private final int[] inputIds;
    private final int contextSize;
    private final int vocabSize;
    private final AtomicInteger inputLength;
    private final AtomicInteger pastTokensSize;

    @Getter
    @Setter
    private int maxNewTokensSize;
    @Getter
    private final long createtime;
    private float[][] scores;
    private final boolean isLogitsAll;

    public UserContext(String id, int contextSize, int vocabSize, boolean isLogitsAll) {
        this.id = id;
        this.contextSize = contextSize;
        this.vocabSize = vocabSize;
        this.inputIds = new int[contextSize];
        this.inputLength = new AtomicInteger(0);
        this.pastTokensSize = new AtomicInteger(0);
        this.createtime = System.currentTimeMillis();
        int length = isLogitsAll ? contextSize : 1;
        this.scores = new float[length][vocabSize];
        this.isLogitsAll = isLogitsAll;
    }

    public int[] getInput() {
        return inputIds;
    }

    public int[] getInputCopy() {
        return ArrayUtils.subarray(inputIds, 0, getInputLength());
    }

    public void appendInput(int[] tokens) {
        System.arraycopy(tokens, 0, inputIds, Math.max(getInputLength(), 0), tokens.length);
        inputLength.addAndGet(tokens.length);
    }

    public void appendInput(int token) {
        inputIds[getInputLength()] = token;
        inputLength.incrementAndGet();
    }

    public int getInputLength() {
        return inputLength.get();
    }

    public int getPastTokensSize() {
        return pastTokensSize.get();
    }

    public void addPastTokensSize(int numbers) {
        pastTokensSize.addAndGet(numbers);
    }

    public void truncate(int keepSize) {
        if (keepSize <= 0 || keepSize >= contextSize) {
            keepSize = contextSize / 2;
        }
        int[] newTokensBuffer = ArrayUtils.subarray(inputIds, keepSize, inputIds.length);
        Arrays.fill(inputIds, 0);
        System.arraycopy(newTokensBuffer, 0, inputIds, 0, newTokensBuffer.length);

        float[][] newScores = ArrayUtils.subarray(scores, keepSize, scores.length);
        int length = isLogitsAll ? contextSize : 1;
        scores = new float[length][vocabSize];
        System.arraycopy(newScores, 0, scores, 0, newScores.length);

        pastTokensSize.set(keepSize);
        inputLength.set(keepSize);
    }

    public void saveScores(float[] values, int evaluateTotalSize) {
        int start = isLogitsAll ? Math.max(getInputLength() - evaluateTotalSize, 0) : 0;
        int end = isLogitsAll ? getInputLength() : 1;
        for (int i = start; i < end; i++) {
            System.arraycopy(values, 0, scores[i], 0, values.length);
        }
    }

    public void updateScores(float[] values) {
        int index = isLogitsAll ? getInputLength() - 1 : 0;
        System.arraycopy(values, 0, scores[index], 0, values.length);
    }

    public float[] getScores() {
        int index = isLogitsAll ? Math.max(getPastTokensSize() - 1, 0) : 0;
        return ArrayUtils.subarray(scores[index], 0, scores[index].length);
    }

    public void destroy() {
        this.pastTokensSize.set(0);
        this.inputLength.set(0);
        Arrays.fill(this.inputIds, 0);
        this.scores = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserContext)) return false;
        UserContext that = (UserContext) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
