package utp.mastertech.mastertech.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import utp.mastertech.mastertech.dto.CambiarPasswordRequest;
import utp.mastertech.mastertech.dto.PerfilDTO;
import utp.mastertech.mastertech.dto.PerfilUpdateRequest;
import utp.mastertech.mastertech.dto.VentaDTO;
import utp.mastertech.mastertech.mapper.VentaMapper;
import utp.mastertech.mastertech.model.Usuario;
import utp.mastertech.mastertech.service.UsuarioService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Datos del cliente autenticado (tabla usuarios): perfil, contraseña e historial de compras. */
@RestController
@RequestMapping("/api/v1/me")
public class MeRestController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private VentaMapper ventaMapper;

    @GetMapping
    public PerfilDTO verPerfil(Authentication auth) {
        return armarPerfil(auth.getName());
    }

    @PutMapping
    public PerfilDTO actualizarPerfil(@Valid @RequestBody PerfilUpdateRequest req, Authentication auth) {
        usuarioService.actualizarDatosPersonales(auth.getName(), req.getNombre().trim(),
                req.getDireccion(), req.getNumeroTelefono());
        return armarPerfil(auth.getName());
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest req,
                                                               Authentication auth) {
        String error = usuarioService.cambiarPassword(auth.getName(),
                req.getPasswordActual(), req.getPasswordNuevo(), req.getPasswordConfirm());
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("error", error));
        }
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada"));
    }

    @GetMapping("/pedidos")
    public List<VentaDTO> historial(Authentication auth) {
        Usuario usuario = usuarioService.obtenerPorEmail(auth.getName());
        return usuarioService.obtenerHistorialCompras(usuario).stream()
                .map(ventaMapper::toDTO)
                .collect(Collectors.toList());
    }

    private PerfilDTO armarPerfil(String email) {
        Usuario u = usuarioService.obtenerPorEmail(email);
        PerfilDTO dto = new PerfilDTO();
        dto.setId(u.getId());
        dto.setNombre(u.getNombre());
        dto.setEmail(u.getEmail());
        dto.setRol(u.getRol());
        dto.setDni(u.getDni());
        dto.setDireccion(u.getDireccion());
        dto.setNumeroTelefono(u.getNumeroTelefono());
        dto.setFechaRegistro(u.getFechaRegistro());
        return dto;
    }
}
