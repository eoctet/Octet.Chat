package chat.octet.model.processor.impl;


import chat.octet.model.processor.LogitsProcessor;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@Slf4j
public class CustomBiasLogitsProcessor implements LogitsProcessor {

    private final Map<Integer, String> logitBias;
    private final int vocabSize;

    public CustomBiasLogitsProcessor(Map<Integer, String> logitBias, int vocabSize) {
        if (logitBias == null) {
            logitBias = Maps.newLinkedHashMap();
        }
        this.logitBias = logitBias;
        this.vocabSize = vocabSize;
    }

    @Override
    public float[] processor(@Nullable int[] inputTokenIds, @Nonnull float[] scores, Object... args) {
        for (Map.Entry<Integer, String> entry : logitBias.entrySet()) {
            int token = entry.getKey();
            String value = entry.getValue();
            if (token >= 0 && token < vocabSize) {
                try {
                    scores[token] = "false".equalsIgnoreCase(value) ? Float.MIN_VALUE : Float.parseFloat(value);
                } catch (Exception e) {
                    log.error("Error: ", e);
                }
            }
        }
        return scores;
    }
}
