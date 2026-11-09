package utp.mastertech.mastertech.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import utp.mastertech.mastertech.model.Usuario;
import utp.mastertech.mastertech.repository.UsuarioRepository;
import utp.mastertech.mastertech.repository.VentaRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService service;

    private Usuario usuario(String passwordHash) {
        Usuario u = new Usuario();
        u.setEmail("ana@x.com");
        u.setPassword(passwordHash);
        return u;
    }

    @Test
    void registrarEncriptaLaPasswordYAsignaRolUser() {
        when(passwordEncoder.encode("Clave123!")).thenReturn("HASH");

        service.registrar("Ana", "ana@x.com", "Clave123!", "12345678", "Calle 1", "987654321");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertEquals("HASH", guardado.getPassword());
        assertEquals("USER", guardado.getRol());
        assertEquals("ana@x.com", guardado.getEmail());
        assertEquals("12345678", guardado.getDni());
    }

    @Test
    void existeEmailDniYTelefono() {
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(new Usuario()));
        when(usuarioRepository.findByDni("11111111")).thenReturn(Optional.empty());
        when(usuarioRepository.findByNumeroTelefono("999999999")).thenReturn(Optional.of(new Usuario()));

        assertTrue(service.existeEmail("ana@x.com"));
        assertFalse(service.existeDni("11111111"));
        assertTrue(service.existeTelefono("999999999"));
    }

    @Test
    void obtenerPorEmailFallaSiNoExiste() {
        when(usuarioRepository.findByEmail("no@x.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.obtenerPorEmail("no@x.com"));
    }

    @Test
    void actualizarDatosPersonalesCambiaSoloEsosCampos() {
        Usuario u = usuario("HASH");
        u.setRol("USER");
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(u));

        service.actualizarDatosPersonales("ana@x.com", "Ana Nueva", "Av. 2", "911111111");

        assertEquals("Ana Nueva", u.getNombre());
        assertEquals("Av. 2", u.getDireccion());
        assertEquals("911111111", u.getNumeroTelefono());
        assertEquals("USER", u.getRol());
        verify(usuarioRepository).save(u);
    }

    @Test
    void cambiarPasswordRechazaLaActualIncorrecta() {
        Usuario u = usuario("HASH");
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("mala", "HASH")).thenReturn(false);

        String error = service.cambiarPassword("ana@x.com", "mala", "NuevaClave1", "NuevaClave1");

        assertEquals("La contraseña actual es incorrecta", error);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarPasswordRechazaSiNoCoinciden() {
        Usuario u = usuario("HASH");
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("actual", "HASH")).thenReturn(true);

        String error = service.cambiarPassword("ana@x.com", "actual", "NuevaClave1", "OtraClave1");

        assertEquals("Las contraseñas nuevas no coinciden", error);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarPasswordRechazaMenosDe8Caracteres() {
        Usuario u = usuario("HASH");
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("actual", "HASH")).thenReturn(true);

        String error = service.cambiarPassword("ana@x.com", "actual", "corta", "corta");

        assertEquals("La nueva contraseña debe tener al menos 8 caracteres", error);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarPasswordCorrectoGuardaElNuevoHashYDevuelveNull() {
        Usuario u = usuario("HASH");
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("actual", "HASH")).thenReturn(true);
        when(passwordEncoder.encode("NuevaClave1")).thenReturn("HASH_NUEVO");

        String error = service.cambiarPassword("ana@x.com", "actual", "NuevaClave1", "NuevaClave1");

        assertNull(error);
        assertEquals("HASH_NUEVO", u.getPassword());
        verify(usuarioRepository).save(u);
    }
}
