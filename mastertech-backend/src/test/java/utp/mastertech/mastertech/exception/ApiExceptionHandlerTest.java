package utp.mastertech.mastertech.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/prueba");

    @Test
    void recursoNoEncontradoDevuelve404ConSuMensaje() {
        ResponseEntity<ErrorResponse> r = handler.handleNotFound(new ResourceNotFoundException("Venta no encontrada: 9"), request);

        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
        assertEquals(404, r.getBody().getStatus());
        assertEquals("Venta no encontrada: 9", r.getBody().getMessage());
        assertEquals("/api/v1/prueba", r.getBody().getPath());
        assertNotNull(r.getBody().getTimestamp());
    }

    @Test
    void argumentoInvalidoDevuelve400() {
        ResponseEntity<ErrorResponse> r = handler.handleBadRequest(new IllegalArgumentException("dato mal"), request);

        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertEquals("dato mal", r.getBody().getMessage());
    }

    @Test
    void stockInsuficienteDevuelve409ConTodosLosErrores() {
        StockInsuficienteException ex = new StockInsuficienteException(List.of("error A", "error B"));

        ResponseEntity<ErrorResponse> r = handler.handleConflict(ex, request);

        assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
        assertEquals("error A | error B", r.getBody().getMessage());
    }

    @Test
    void conflictoDevuelve409() {
        ResponseEntity<ErrorResponse> r = handler.handleConflict(new ConflictException("El correo ya está registrado"), request);

        assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
        assertEquals("El correo ya está registrado", r.getBody().getMessage());
    }

    @Test
    void violacionDeIntegridadDevuelve409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("x", new RuntimeException("duplicado"));

        ResponseEntity<ErrorResponse> r = handler.handleIntegrity(ex, request);

        assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
    }

    @Test
    void imagenMuyGrandeDevuelve413() {
        ResponseEntity<ErrorResponse> r = handler.handleMaxUpload(request);

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, r.getStatusCode());
        assertTrue(r.getBody().getMessage().contains("5 MB"));
    }

    @Test
    void metodoNoPermitidoDevuelve405() {
        ResponseEntity<ErrorResponse> r = handler.handleMethod(request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, r.getStatusCode());
    }

    @Test
    void fallosDeStripeYStorageDevuelven502() {
        ResponseEntity<ErrorResponse> pago = handler.handleExterno(new PagoException("Stripe caído", new RuntimeException()), request);
        ResponseEntity<ErrorResponse> storage = handler.handleExterno(new StorageException("bucket caído"), request);

        assertEquals(HttpStatus.BAD_GATEWAY, pago.getStatusCode());
        assertEquals(HttpStatus.BAD_GATEWAY, storage.getStatusCode());
        assertEquals("bucket caído", storage.getBody().getMessage());
    }

    @Test
    void errorInesperadoDevuelve500SinFiltrarElDetalleInterno() {
        ResponseEntity<ErrorResponse> r = handler.handleGlobal(new RuntimeException("password=secreto"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
        assertFalse(r.getBody().getMessage().contains("secreto"));
    }
}
