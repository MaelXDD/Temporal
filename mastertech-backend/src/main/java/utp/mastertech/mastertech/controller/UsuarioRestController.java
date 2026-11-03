package utp.mastertech.mastertech.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import utp.mastertech.mastertech.dto.UsuarioDTO;
import utp.mastertech.mastertech.exception.ConflictException;
import utp.mastertech.mastertech.exception.ResourceNotFoundException;
import utp.mastertech.mastertech.mapper.UsuarioMapper;
import utp.mastertech.mastertech.model.Usuario;
import utp.mastertech.mastertech.repository.UsuarioRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Administración de usuarios (solo ADMIN, ver SecurityConfig). */
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioRestController {

    private static final Set<String> ROLES = Set.of("USER", "ADMIN");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioMapper usuarioMapper;

    @GetMapping
    public List<UsuarioDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(usuarioMapper::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public UsuarioDTO obtenerPorId(@PathVariable Long id) {
        return usuarioRepository.findById(id)
                .map(usuarioMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> crearUsuario(@Valid @RequestBody UsuarioDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        validarRol(dto.getRol());
        Usuario usuario = usuarioMapper.toEntity(dto);
        usuario.setId(null);
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioMapper.toDTO(usuarioRepository.save(usuario)));
    }

    @PutMapping("/{id}")
    public UsuarioDTO actualizarUsuario(@PathVariable Long id,
                                        @Valid @RequestBody UsuarioDTO dto,
                                        Authentication auth) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        validarRol(dto.getRol());
        validarNoDegradarseASiMismo(usuario, dto.getRol(), auth);
        usuarioMapper.updateEntityFromDTO(dto, usuario);
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        return usuarioMapper.toDTO(usuarioRepository.save(usuario));
    }

    @PatchMapping("/{id}")
    public UsuarioDTO actualizarParcial(@PathVariable Long id,
                                        @RequestBody Map<String, Object> cambios,
                                        Authentication auth) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        if (cambios.containsKey("rol")) {
            String rol = String.valueOf(cambios.get("rol"));
            validarRol(rol);
            validarNoDegradarseASiMismo(usuario, rol, auth);
            usuario.setRol(rol);
        }
        if (cambios.containsKey("nombre")) usuario.setNombre(String.valueOf(cambios.get("nombre")));
        if (cambios.containsKey("direccion")) usuario.setDireccion(String.valueOf(cambios.get("direccion")));
        if (cambios.containsKey("numeroTelefono")) usuario.setNumeroTelefono(String.valueOf(cambios.get("numeroTelefono")));
        return usuarioMapper.toDTO(usuarioRepository.save(usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id, Authentication auth) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        if (usuario.getEmail().equalsIgnoreCase(auth.getName())) {
            throw new ConflictException("No puedes eliminar tu propia cuenta.");
        }
        usuarioRepository.delete(usuario);
        return ResponseEntity.noContent().build();
    }

    private void validarRol(String rol) {
        if (rol != null && !ROLES.contains(rol)) {
            throw new IllegalArgumentException("Rol no válido. Usa USER o ADMIN.");
        }
    }

    private void validarNoDegradarseASiMismo(Usuario objetivo, String rolNuevo, Authentication auth) {
        if (rolNuevo != null && !"ADMIN".equals(rolNuevo)
                && objetivo.getEmail().equalsIgnoreCase(auth.getName())) {
            throw new ConflictException("No puedes quitarte a ti mismo el rol de administrador.");
        }
    }
}
