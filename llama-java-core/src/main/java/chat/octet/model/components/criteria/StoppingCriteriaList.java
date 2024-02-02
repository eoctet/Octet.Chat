package chat.octet.model.components.criteria;


import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Stopping criteria list
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
@Slf4j
public class StoppingCriteriaList implements StoppingCriteria {

    private final Set<StoppingCriteria> criteriaSet;

    public StoppingCriteriaList() {
        this.criteriaSet = Sets.newHashSet();
    }

    public StoppingCriteriaList add(StoppingCriteria criteria) {
        for (StoppingCriteria c : criteriaSet) {
            if (c.getClass() == criteria.getClass()) {
                return this;
            }
        }
        this.criteriaSet.add(criteria);
        return this;
    }

    public boolean isEmpty() {
        return this.criteriaSet.isEmpty();
    }

    @Override
    public boolean criteria(@Nullable int[] inputTokenIds, @Nonnull float[] scores, Object... args) {
        for (StoppingCriteria criteria : criteriaSet) {
            if (criteria.criteria(inputTokenIds, scores, args)) {
                log.debug("Matched stop criteria, criteria name: {}.", criteria.getClass().getName());
                return true;
            }
        }
        return false;
    }
}
