package utp.mastertech.mastertech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import utp.mastertech.mastertech.model.Usuario;
import utp.mastertech.mastertech.model.Venta;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {


    // Para el historial de compras del perfil (más reciente primero)
    List<Venta> findByUsuarioOrderByFechaDesc(Usuario usuario);

    Optional<Venta> findByNumeroOrden(String numeroOrden);


    @Query("SELECT v FROM Venta v LEFT JOIN v.usuario u WHERE " +
            "CAST(v.id AS string) LIKE %:keyword% OR " +
            "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.numeroOrden) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Venta> findByKeyword(@Param("keyword") String keyword);


}
