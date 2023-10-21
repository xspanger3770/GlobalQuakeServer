package gqserver.exception;

public class UnknownPacketException extends FatalIOException {
    public UnknownPacketException(String message, Throwable cause) {
        super(message, cause);
    }
}
