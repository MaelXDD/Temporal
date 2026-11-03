package utp.mastertech.mastertech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import utp.mastertech.mastertech.model.Perfil;
import utp.mastertech.mastertech.model.Usuario;

import java.util.Optional;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    Optional<Perfil> findByUsuario(Usuario usuario);

    Optional<Perfil> findByUsuarioId(Long usuarioId);

    boolean existsByUsuario(Usuario usuario);
}
