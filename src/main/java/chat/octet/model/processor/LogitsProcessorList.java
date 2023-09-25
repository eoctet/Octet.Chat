package chat.octet.model.processor;


import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;

public final class LogitsProcessorList extends ArrayList<LogitsProcessor> implements LogitsProcessor {

    public LogitsProcessorList() {
    }

    public LogitsProcessorList(Collection<? extends LogitsProcessor> c) {
        super(c);
    }

    @Override
    public float[] processor(@Nullable int[] inputTokenIds, @Nonnull float[] scores, Object... args) {
        Preconditions.checkNotNull(scores, "Scores cannot be null");

        float[] result = null;
        for (LogitsProcessor pro : this) {
            result = pro.processor(inputTokenIds, scores, args);
        }
        return result;
    }
}
