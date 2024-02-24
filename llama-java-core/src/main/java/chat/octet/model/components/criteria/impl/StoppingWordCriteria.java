package chat.octet.model.components.criteria.impl;

import chat.octet.model.LlamaService;
import chat.octet.model.beans.Token;
import chat.octet.model.components.criteria.StoppingCriteria;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;


public class StoppingWordCriteria implements StoppingCriteria {

    private final List<int[]> stoppingTokens;

    public StoppingWordCriteria(String... words) {
        Preconditions.checkNotNull(words, "Stopping words cannot be null");
        this.stoppingTokens = Lists.newArrayList();

        for (String word : words) {
            if (NumberUtils.isParsable(word)) {
                stoppingTokens.add(new int[]{Integer.parseInt(word)});
            } else {
                int[] tokens = LlamaService.tokenize(word, false, true);
                stoppingTokens.add(tokens);
            }
        }
    }

    @Override
    public boolean criteria(@Nullable int[] inputTokenIds, @Nonnull float[] scores, Object... args) {
        if (args != null && args.length == 1) {
            @SuppressWarnings("unchecked")
            List<Token> generateTokens = (List<Token>) args[0];

            for (int[] tokens : stoppingTokens) {
                int length = tokens.length;
                if (length > generateTokens.size()) {
                    continue;
                }
                int[] lastTokens = generateTokens.subList(generateTokens.size() - length, generateTokens.size()).stream().mapToInt(Token::getId).toArray();
                if (Arrays.equals(tokens, lastTokens)) {
                    return true;
                }
            }
        }
        return false;
    }

}
