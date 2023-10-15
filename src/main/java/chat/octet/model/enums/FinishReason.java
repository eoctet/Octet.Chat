package chat.octet.model.enums;

import chat.octet.model.components.criteria.StoppingCriteria;

/**
 * Token generate status
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public enum FinishReason {
    /**
     * Completed generation.
     */
    FINISHED,
    /**
     * Generation has exceeded the maximum token limit and has been truncated.
     */
    LENGTH,
    /**
     * Generation stopped by StoppingCriteria.
     *
     * @see StoppingCriteria
     */
    STOP,
    /**
     * Default type.
     */
    NONE,
    /**
     * Unknown type, no available token state.
     */
    UNKNOWN,
    /**
     * Generation has exceeded the maximum context limit and has been truncated.
     */
    TRUNCATED;

    /**
     * Check if the token has been completed else return false.
     * Finished reason: FINISHED / LENGTH / STOP / TRUNCATED
     *
     * @return boolean
     */
    public boolean isFinished() {
        return this == FINISHED || this == LENGTH || this == STOP || this == TRUNCATED;
    }
}
