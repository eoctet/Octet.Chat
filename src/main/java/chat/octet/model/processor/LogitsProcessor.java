package chat.octet.model.processor;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface LogitsProcessor {

    float[] processor(@Nullable int[] inputTokenIds, @Nonnull float[] scores, Object... args);

}
