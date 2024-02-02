package chat.octet.model.components.processor;


import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

/**
 * Stopping criteria list
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public final class LogitsProcessorList implements LogitsProcessor {

    private final Set<LogitsProcessor> logitsProcessors;

    public LogitsProcessorList() {
        this.logitsProcessors = Sets.newHashSet();
    }

    public LogitsProcessorList add(LogitsProcessor processor) {
        for (LogitsProcessor p : logitsProcessors) {
            if (p.getClass() == processor.getClass()) {
                return this;
            }
        }
        this.logitsProcessors.add(processor);
        return this;
    }

    public boolean isEmpty() {
        return this.logitsProcessors.isEmpty();
    }

    @Override
    public float[] processor(@Nullable int[] inputTokenIds, @Nonnull float[] scores, Object... args) {
        Preconditions.checkNotNull(scores, "Scores cannot be null");

        float[] result = null;
        for (LogitsProcessor pro : logitsProcessors) {
            result = pro.processor(inputTokenIds, scores, args);
        }
        return Optional.ofNullable(result).orElse(scores);
    }
}
