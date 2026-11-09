package utp.mastertech.mastertech.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import utp.mastertech.mastertech.exception.ApiExceptionHandler;
import utp.mastertech.mastertech.model.Usuario;
import utp.mastertech.mastertech.repository.UsuarioRepository;
import utp.mastertech.mastertech.security.JwtUtil;
import utp.mastertech.mastertech.service.UsuarioService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthRestControllerTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthRestController controller;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(7L);
        u.setEmail("ana@x.com");
        u.setNombre("Ana");
        u.setPassword("HASH");
        u.setRol("USER");
        return u;
    }

    private static final String REGISTRO_VALIDO =
            "{\"nombre\":\"Ana Perez\",\"email\":\"ana@x.com\",\"password\":\"Clave123!\","
                    + "\"dni\":\"12345678\",\"direccion\":\"Calle 1\",\"numeroTelefono\":\"987654321\"}";

    @Test
    void loginCorrectoDevuelveElToken() throws Exception {
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(usuario()));
        when(passwordEncoder.matches("Clave123!", "HASH")).thenReturn(true);
        when(jwtUtil.generarToken("ana@x.com", "USER")).thenReturn("TOKEN123");

        String cuerpo = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" ana@x.com \",\"password\":\"Clave123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("TOKEN123"));
        assertTrue(cuerpo.contains("\"rol\":\"USER\""));
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(usuario()));
        when(passwordEncoder.matches("mala", "HASH")).thenReturn(false);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@x.com\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void loginConUsuarioInexistenteDevuelve401() throws Exception {
        when(usuarioRepository.findByEmail("no@x.com")).thenReturn(Optional.empty());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no@x.com\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSinEmailDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registroCorrectoDevuelve201() throws Exception {
        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_VALIDO))
                .andExpect(status().isCreated());

        verify(usuarioService).registrar("Ana Perez", "ana@x.com", "Clave123!",
                "12345678", "Calle 1", "987654321");
    }

    @Test
    void registroConEmailRepetidoDevuelve409() throws Exception {
        when(usuarioService.existeEmail("ana@x.com")).thenReturn(true);

        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_VALIDO))
                .andExpect(status().isConflict());

        verify(usuarioService, never()).registrar(anyString(), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void registroConDniRepetidoDevuelve409() throws Exception {
        when(usuarioService.existeDni("12345678")).thenReturn(true);

        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void registroConPasswordDebilDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_VALIDO.replace("Clave123!", "debil")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void meDevuelveLosDatosDeLaSesion() throws Exception {
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(usuario()));

        mvc.perform(get("/api/auth/me")
                        .principal(new UsernamePasswordAuthenticationToken("ana@x.com", null)))
                .andExpect(status().isOk());
    }

    @Test
    void meConUsuarioEliminadoDevuelve401() throws Exception {
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.empty());

        mvc.perform(get("/api/auth/me")
                        .principal(new UsernamePasswordAuthenticationToken("ana@x.com", null)))
                .andExpect(status().isUnauthorized());
    }
}
