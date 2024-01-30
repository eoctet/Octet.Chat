package chat.octet.model.components.criteria.impl;

import chat.octet.model.beans.Token;
import chat.octet.model.components.criteria.StoppingCriteria;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;


public class StoppingWordCriteria implements StoppingCriteria {

    private final List<String> stoppingWords;

    public StoppingWordCriteria(String... stoppingWords) {
        Preconditions.checkNotNull(stoppingWords, "Stopping words cannot be null");
        this.stoppingWords = Arrays.asList(stoppingWords);
    }

    @Override
    public boolean criteria(@Nullable int[] inputTokenIds, @Nonnull float[] scores, Object... args) {
        if (args != null && args.length == 1) {
            Token token = (Token) args[0];
            for (String word : stoppingWords) {
                if (NumberUtils.isParsable(word)) {
                    return Integer.parseInt(word) == token.getId();
                } else {
                    return token.getText().equals(word);
                }
            }
        }
        return false;
    }
}
