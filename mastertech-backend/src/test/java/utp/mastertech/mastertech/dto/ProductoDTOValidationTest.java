package utp.mastertech.mastertech.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ProductoDTOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private ProductoDTO valido() {
        ProductoDTO d = new ProductoDTO();
        d.setNombre("Mouse");
        d.setPrecio(50.0);
        d.setStock(10);
        d.setCategoriaId(1L);
        return d;
    }

    private Set<String> mensajes(ProductoDTO d) {
        return validator.validate(d).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    void productoCorrectoNoTieneErrores() {
        assertTrue(validator.validate(valido()).isEmpty());
    }

    @Test
    void exigeNombre() {
        ProductoDTO d = valido();
        d.setNombre(" ");
        assertTrue(mensajes(d).contains("El nombre del producto no puede estar en blanco"));
    }

    @Test
    void noAceptaPrecioNegativo() {
        ProductoDTO d = valido();
        d.setPrecio(-1.0);
        assertTrue(mensajes(d).contains("El precio debe ser 0 o mayor"));
    }

    @Test
    void noAceptaStockNegativo() {
        ProductoDTO d = valido();
        d.setStock(-5);
        assertTrue(mensajes(d).contains("El stock no puede ser negativo"));
    }

    @Test
    void exigeCategoria() {
        ProductoDTO d = valido();
        d.setCategoriaId(null);
        assertTrue(mensajes(d).contains("La categoría es obligatoria"));
    }
}
