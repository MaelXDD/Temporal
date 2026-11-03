package utp.mastertech.mastertech.exception;

/** Operación rechazada por el estado actual de los datos (HTTP 409). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
