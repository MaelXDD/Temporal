package utp.mastertech.mastertech.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import utp.mastertech.mastertech.model.Usuario;
import utp.mastertech.mastertech.repository.UsuarioRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void cargaElUsuarioConSuRolComoAuthority() {
        Usuario u = new Usuario();
        u.setEmail("ana@x.com");
        u.setPassword("HASH");
        u.setNombre("Ana");
        u.setRol("ADMIN");
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(u));

        UserDetails details = service.loadUserByUsername("ana@x.com");

        assertEquals("ana@x.com", details.getUsername());
        assertEquals("HASH", details.getPassword());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(details.isEnabled());
    }

    @Test
    void lanzaExcepcionSiElUsuarioNoExiste() {
        when(usuarioRepository.findByEmail("no@x.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("no@x.com"));
    }
}
