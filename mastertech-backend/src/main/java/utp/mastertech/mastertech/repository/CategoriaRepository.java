package utp.mastertech.mastertech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utp.mastertech.mastertech.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}