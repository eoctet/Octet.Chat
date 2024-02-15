package chat.octet.exceptions;


public class PluginExecuteException extends RuntimeException {

    public PluginExecuteException(String message) {
        super(message);
    }

    public PluginExecuteException(String message, Throwable cause) {
        super(message, cause);
    }
}