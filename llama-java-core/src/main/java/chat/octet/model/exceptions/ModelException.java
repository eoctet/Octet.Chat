package chat.octet.model.exceptions;

/**
 * Model exception
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public class ModelException extends RuntimeException {

    public ModelException(String message) {
        super(message);
    }

    public ModelException(String message, Throwable cause) {
        super(message, cause);
    }
}