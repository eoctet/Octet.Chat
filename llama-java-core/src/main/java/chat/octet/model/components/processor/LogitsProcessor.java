package chat.octet.model.components.processor;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Customize a processor to adjust the probability distribution of words and control the generation of model inference results.
 * <p>Note: Referenced the Transformers document implementation.</p>
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public interface LogitsProcessor {

    /**
     * Logits processor
     *
     * @param inputTokenIds Indices of input sequence tokens in the vocabulary.
     * @param scores        Prediction scores of a language modeling head. These can be logits for each vocabulary.
     * @param args          Specific args to a logits processor.
     * @return float[] The processed prediction scores.
     */
    float[] processor(@Nullable int[] inputTokenIds, @Nonnull float[] scores, Object... args);

}
