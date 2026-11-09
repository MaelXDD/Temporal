package utp.mastertech.mastertech.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private FilterChain chain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinCabeceraContinuaSinAutenticar() throws Exception {
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void cabeceraQueNoEsBearerSeIgnora() throws Exception {
        request.addHeader("Authorization", "Basic abc");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void tokenInvalidoResponde401YCortaLaCadena() throws Exception {
        request.addHeader("Authorization", "Bearer malo");
        when(jwtUtil.validarToken("malo")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("inválido o expirado"));
        verifyNoInteractions(chain);
    }

    @Test
    void tokenValidoAutenticaConElRolPrefijado() throws Exception {
        request.addHeader("Authorization", "Bearer bueno");
        when(jwtUtil.validarToken("bueno")).thenReturn(true);
        when(jwtUtil.extraerEmail("bueno")).thenReturn("ana@x.com");
        when(jwtUtil.extraerRol("bueno")).thenReturn("ADMIN");

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("ana@x.com", auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(chain).doFilter(request, response);
    }

    @Test
    void siElRolYaTraePrefijoNoLoDuplica() throws Exception {
        request.addHeader("Authorization", "Bearer bueno");
        when(jwtUtil.validarToken("bueno")).thenReturn(true);
        when(jwtUtil.extraerEmail("bueno")).thenReturn("ana@x.com");
        when(jwtUtil.extraerRol("bueno")).thenReturn("ROLE_USER");

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }
}
