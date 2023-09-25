package chat.octet.model.criteria;


import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;

@Slf4j
public class StoppingCriteriaList extends ArrayList<StoppingCriteria> implements StoppingCriteria {

    public StoppingCriteriaList() {
    }

    public StoppingCriteriaList(Collection<? extends StoppingCriteria> c) {
        super(c);
    }

    @Override
    public boolean criteria(@Nullable int[] inputTokenIds, @Nonnull float[] scores, Object... args) {
        for (StoppingCriteria criteria : this) {
            if (criteria.criteria(inputTokenIds, scores, args)) {
                log.debug("Matched stop criteria, criteria name: {}.", criteria.getClass().getName());
                return true;
            }
        }
        return false;
    }
}
