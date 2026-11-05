package utp.mastertech.mastertech.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utp.mastertech.mastertech.model.Usuario;
import utp.mastertech.mastertech.model.Venta;
import utp.mastertech.mastertech.repository.UsuarioRepository;
import utp.mastertech.mastertech.repository.VentaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final VentaRepository ventaRepository;
    private final PasswordEncoder passwordEncoder;

    public void registrar(String nombre, String email, String password,
                          String dni, String direccion, String numeroTelefono) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol("USER");
        usuario.setDni(dni);
        usuario.setDireccion(direccion);
        usuario.setNumeroTelefono(numeroTelefono);
        usuarioRepository.save(usuario);
    }

    public boolean existeEmail(String email) {
        return usuarioRepository.findByEmail(email).isPresent();
    }
    public boolean existeDni(String dni) {
        return usuarioRepository.findByDni(dni).isPresent();
    }
    public boolean existeTelefono(String numeroTelefono) {
        return usuarioRepository.findByNumeroTelefono(numeroTelefono).isPresent();
    }

    // ---- "Mi perfil": todo vive directamente en la tabla usuarios ----

    public Usuario obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
    }

    @Transactional
    public void actualizarDatosPersonales(String email, String nombre, String direccion, String telefono) {
        Usuario usuario = obtenerPorEmail(email);
        usuario.setNombre(nombre);
        usuario.setDireccion(direccion);
        usuario.setNumeroTelefono(telefono);
        usuarioRepository.save(usuario);
    }

    /** Devuelve null si todo salió bien, o el mensaje de error. */
    @Transactional
    public String cambiarPassword(String email, String passwordActual,
                                  String passwordNuevo, String passwordConfirm) {
        Usuario usuario = obtenerPorEmail(email);

        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            return "La contraseña actual es incorrecta";
        }
        if (!passwordNuevo.equals(passwordConfirm)) {
            return "Las contraseñas nuevas no coinciden";
        }
        if (passwordNuevo.length() < 8) {
            return "La nueva contraseña debe tener al menos 8 caracteres";
        }

        usuario.setPassword(passwordEncoder.encode(passwordNuevo));
        usuarioRepository.save(usuario);
        return null;
    }

    public List<Venta> obtenerHistorialCompras(Usuario usuario) {
        return ventaRepository.findByUsuarioOrderByFechaDesc(usuario);
    }
}
