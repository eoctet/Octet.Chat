package chat.octet.model.exceptions;

/**
 * Generation exception
 *
 * @author <a href="https://github.com/eoctet">William</a>
 */
public class GenerationException extends RuntimeException {

    public GenerationException(String message) {
        super(message);
    }

    public GenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}