package chat.octet.model.beans;

/**
 * Token generate status
 *
 * @author william
 * @version 1.0
 */
public enum FinishReason {
    FINISHED, LENGTH, STOP, NONE;

    public boolean isFinished() {
        return this == FINISHED || this == LENGTH || this == STOP;
    }
}
