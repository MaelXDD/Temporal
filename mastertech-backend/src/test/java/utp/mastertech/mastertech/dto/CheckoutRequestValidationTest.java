package utp.mastertech.mastertech.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private ItemPedidoDTO item(Long id, Integer cantidad) {
        ItemPedidoDTO i = new ItemPedidoDTO();
        i.setProductoId(id);
        i.setCantidad(cantidad);
        return i;
    }

    private Set<String> mensajes(CheckoutRequest r) {
        return validator.validate(r).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    void carritoConItemsValidosNoTieneErrores() {
        CheckoutRequest r = new CheckoutRequest();
        r.setItems(List.of(item(1L, 2)));

        assertTrue(validator.validate(r).isEmpty());
    }

    @Test
    void carritoVacioEsInvalido() {
        CheckoutRequest r = new CheckoutRequest();
        r.setItems(List.of());

        assertTrue(mensajes(r).contains("El carrito está vacío"));
    }

    @Test
    void laCantidadMinimaEs1() {
        CheckoutRequest r = new CheckoutRequest();
        r.setItems(List.of(item(1L, 0)));

        assertTrue(mensajes(r).contains("La cantidad mínima es 1"));
    }

    @Test
    void exigeProductoYCantidad() {
        CheckoutRequest r = new CheckoutRequest();
        r.setItems(List.of(item(null, null)));

        Set<String> m = mensajes(r);
        assertTrue(m.contains("El producto es obligatorio"));
        assertTrue(m.contains("La cantidad es obligatoria"));
    }

    @Test
    void confirmarPagoExigeElIdDeSesion() {
        ConfirmarPagoRequest r = new ConfirmarPagoRequest();
        r.setSessionId(" ");

        assertFalse(validator.validate(r).isEmpty());
    }
}
