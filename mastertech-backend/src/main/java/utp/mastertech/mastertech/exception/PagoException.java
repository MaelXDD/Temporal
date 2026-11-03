package utp.mastertech.mastertech.exception;

/** Fallo al hablar con la pasarela de pago (Stripe). */
public class PagoException extends RuntimeException {
    public PagoException(String message, Throwable cause) {
        super(message, cause);
    }
}
