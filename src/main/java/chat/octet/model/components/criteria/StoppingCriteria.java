package chat.octet.model.components.criteria;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface StoppingCriteria {

    boolean criteria(@Nullable int[] inputTokenIds, @Nonnull float[] scores, Object... args);

}
