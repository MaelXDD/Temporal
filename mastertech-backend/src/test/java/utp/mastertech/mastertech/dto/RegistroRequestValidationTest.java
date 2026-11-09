package utp.mastertech.mastertech.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RegistroRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private RegistroRequest valido() {
        RegistroRequest r = new RegistroRequest();
        r.setNombre("Ana Perez");
        r.setEmail("ana@x.com");
        r.setPassword("Clave123!");
        r.setDni("12345678");
        r.setDireccion("Calle 1");
        r.setNumeroTelefono("987654321");
        return r;
    }

    private Set<String> mensajes(RegistroRequest r) {
        return validator.validate(r).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    void unRegistroCorrectoNoTieneErrores() {
        assertTrue(validator.validate(valido()).isEmpty());
    }

    @Test
    void rechazaPasswordSinMayusculaNumeroOEspecial() {
        RegistroRequest r = valido();
        r.setPassword("clavesimple");

        assertTrue(mensajes(r).contains("Mínimo 8 caracteres, con una mayúscula, un número y un carácter especial"));
    }

    @Test
    void rechazaDniQueNoTiene8Digitos() {
        RegistroRequest r = valido();
        r.setDni("123");

        assertTrue(mensajes(r).contains("El DNI debe tener exactamente 8 dígitos"));
    }

    @Test
    void rechazaTelefonoQueNoTiene9Digitos() {
        RegistroRequest r = valido();
        r.setNumeroTelefono("12345");

        assertTrue(mensajes(r).contains("El teléfono debe tener 9 dígitos"));
    }

    @Test
    void rechazaEmailInvalido() {
        RegistroRequest r = valido();
        r.setEmail("no-es-un-correo");

        assertTrue(mensajes(r).contains("El formato del email no es válido"));
    }
}
