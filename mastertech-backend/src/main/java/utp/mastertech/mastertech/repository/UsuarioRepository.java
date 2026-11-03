    package utp.mastertech.mastertech.repository;

    import org.springframework.data.jpa.repository.JpaRepository;
    import utp.mastertech.mastertech.model.Usuario;
    import java.util.Optional;

    public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
        Optional<Usuario> findByEmail(String email);
        Optional<Usuario> findByDni(String dni);
        Optional<Usuario> findByNumeroTelefono(String numeroTelefono);
    }