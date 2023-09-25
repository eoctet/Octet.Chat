package chat.octet.model.processor.impl;


import chat.octet.model.processor.LogitsProcessor;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NoBadWordsLogitsProcessor implements LogitsProcessor {

    private final int[] badWordsTokenIds;

    public NoBadWordsLogitsProcessor(int[] badWordsTokenIds) {
        Preconditions.checkNotNull(badWordsTokenIds, "Bad word tokens cannot be null");
        this.badWordsTokenIds = badWordsTokenIds;
    }

    @Override
    public float[] processor(@Nullable int[] inputTokenIds, @Nonnull float[] scores, Object... args) {
        for (int id : badWordsTokenIds) {
            scores[id] = Float.MIN_VALUE;
        }
        return scores;
    }
}
