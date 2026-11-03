package utp.mastertech.mastertech.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import utp.mastertech.mastertech.model.Usuario;
import utp.mastertech.mastertech.repository.UsuarioRepository;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
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
}