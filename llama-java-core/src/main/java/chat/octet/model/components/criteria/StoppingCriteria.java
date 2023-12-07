package chat.octet.model.components.criteria;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Customize a controller to implement stop rule control for model inference.
 * <p>Note: Referenced the Transformers document implementation.</p>
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public interface StoppingCriteria {

    /**
     * Stopping criteria
     *
     * @param inputTokenIds Indices of input sequence tokens in the vocabulary.
     * @param scores        Prediction scores of a language modeling head. These can be logits for each vocabulary.
     * @param args          Specific args to a stopping criteria.
     * @return boolean `False` indicates we should continue, `True` indicates we should stop.
     */
    boolean criteria(@Nullable int[] inputTokenIds, @Nonnull float[] scores, Object... args);

}
