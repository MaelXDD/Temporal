package utp.mastertech.mastertech.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import utp.mastertech.mastertech.dto.LoginRequest;
import utp.mastertech.mastertech.dto.RegistroRequest;
import utp.mastertech.mastertech.exception.ConflictException;
import utp.mastertech.mastertech.model.Usuario;
import utp.mastertech.mastertech.repository.UsuarioRepository;
import utp.mastertech.mastertech.security.JwtUtil;
import utp.mastertech.mastertech.service.UsuarioService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest credenciales) {
        String email = credenciales.getEmail().trim();

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isEmpty()
                || !passwordEncoder.matches(credenciales.getPassword(), usuarioOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas"));
        }

        Usuario usuario = usuarioOpt.get();
        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol());
        return ResponseEntity.ok(respuestaSesion(token, usuario));
    }

    @PostMapping("/registro")
    public ResponseEntity<Map<String, String>> registro(@Valid @RequestBody RegistroRequest req) {
        String email = req.getEmail().trim();
        if (usuarioService.existeEmail(email)) {
            throw new ConflictException("El correo ya está registrado");
        }
        if (usuarioService.existeDni(req.getDni())) {
            throw new ConflictException("El DNI ya está registrado");
        }
        if (usuarioService.existeTelefono(req.getNumeroTelefono())) {
            throw new ConflictException("El número de teléfono ya está registrado");
        }
        usuarioService.registrar(req.getNombre().trim(), email, req.getPassword(),
                req.getDni(), req.getDireccion(), req.getNumeroTelefono());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Cuenta creada correctamente"));
    }

    /** Devuelve los datos de la sesión actual (el front lo usa para validar el token guardado). */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(respuestaSesion(null, u)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Sesión no válida")));
    }

    private Map<String, Object> respuestaSesion(String token, Usuario u) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        if (token != null) body.put("token", token);
        body.put("id", u.getId());
        body.put("email", u.getEmail());
        body.put("nombre", u.getNombre());
        body.put("rol", u.getRol());
        return body;
    }
}
