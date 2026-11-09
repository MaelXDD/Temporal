package utp.mastertech.mastertech.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "clave-de-prueba-de-al-menos-32-caracteres!!";

    private final JwtUtil jwt = new JwtUtil(SECRET, 60_000);

    @Test
    void generaUnTokenValidoConEmailYRol() {
        String token = jwt.generarToken("ana@x.com", "ADMIN");

        assertTrue(jwt.validarToken(token));
        assertEquals("ana@x.com", jwt.extraerEmail(token));
        assertEquals("ADMIN", jwt.extraerRol(token));
    }

    @Test
    void rechazaUnTokenInventado() {
        assertFalse(jwt.validarToken("esto.no.esunjwt"));
    }

    @Test
    void rechazaUnTokenVacioONulo() {
        assertFalse(jwt.validarToken(""));
        assertFalse(jwt.validarToken(null));
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        JwtUtil otro = new JwtUtil("otra-clave-distinta-de-al-menos-32-caracteres", 60_000);
        String tokenAjeno = otro.generarToken("ana@x.com", "USER");

        assertFalse(jwt.validarToken(tokenAjeno));
    }

    @Test
    void rechazaUnTokenExpirado() {
        JwtUtil expirado = new JwtUtil(SECRET, -1_000);
        String token = expirado.generarToken("ana@x.com", "USER");

        assertFalse(jwt.validarToken(token));
    }

    @Test
    void exigeUnSecretDeAlMenos32Caracteres() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("corta", 1_000));
    }
}
