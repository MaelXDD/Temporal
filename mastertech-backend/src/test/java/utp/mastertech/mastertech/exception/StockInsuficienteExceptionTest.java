package utp.mastertech.mastertech.exception;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StockInsuficienteExceptionTest {

    @Test
    void conUnMensajeGuardaUnSoloError() {
        StockInsuficienteException ex = new StockInsuficienteException("sin stock");

        assertEquals(List.of("sin stock"), ex.getErrores());
        assertEquals("sin stock", ex.getMessage());
    }

    @Test
    void conVariosErroresLosUneConBarra() {
        StockInsuficienteException ex = new StockInsuficienteException(List.of("a", "b", "c"));

        assertEquals(3, ex.getErrores().size());
        assertEquals("a | b | c", ex.getMessage());
    }
}
