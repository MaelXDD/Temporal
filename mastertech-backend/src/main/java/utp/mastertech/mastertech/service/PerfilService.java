package utp.mastertech.mastertech.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utp.mastertech.mastertech.model.Perfil;
import utp.mastertech.mastertech.model.Usuario;
import utp.mastertech.mastertech.model.Venta;
import utp.mastertech.mastertech.repository.PerfilRepository;
import utp.mastertech.mastertech.repository.UsuarioRepository;
import utp.mastertech.mastertech.repository.VentaRepository;

import java.util.List;

@Service
public class PerfilService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    public Usuario obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
    }


    public Perfil obtenerOCrearPerfil(Usuario usuario) {
        return perfilRepository.findByUsuario(usuario).orElseGet(() -> {
            Perfil nuevo = new Perfil();
            nuevo.setUsuario(usuario);
            nuevo.setBiografia("");
            return perfilRepository.save(nuevo);
        });
    }


    @Transactional
    public void actualizarDatosPersonales(String email, String nombre,
                                          String direccion, String telefono,
                                          String biografia) {
        Usuario usuario = obtenerUsuarioPorEmail(email);
        usuario.setNombre(nombre);
        usuario.setDireccion(direccion);
        usuario.setNumeroTelefono(telefono);
        usuarioRepository.save(usuario);

        Perfil perfil = obtenerOCrearPerfil(usuario);
        perfil.setBiografia(biografia);
        perfilRepository.save(perfil);
    }


    @Transactional
    public String cambiarPassword(String email, String passwordActual,
                                  String passwordNuevo, String passwordConfirm) {
        Usuario usuario = obtenerUsuarioPorEmail(email);

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
