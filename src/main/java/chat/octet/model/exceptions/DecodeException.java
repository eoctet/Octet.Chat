package chat.octet.model.exceptions;

/**
 * Batch decode exception
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public class DecodeException extends RuntimeException {

    public DecodeException(String message) {
        super(message);
    }

    public DecodeException(String message, Throwable cause) {
        super(message, cause);
    }
}