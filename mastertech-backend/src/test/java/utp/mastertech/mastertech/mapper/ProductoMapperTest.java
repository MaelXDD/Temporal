package utp.mastertech.mastertech.mapper;

import org.junit.jupiter.api.Test;
import utp.mastertech.mastertech.dto.ProductoDTO;
import utp.mastertech.mastertech.model.Categoria;
import utp.mastertech.mastertech.model.Producto;

import static org.junit.jupiter.api.Assertions.*;

class ProductoMapperTest {

    private final ProductoMapper mapper = new ProductoMapper();

    private Producto producto() {
        Categoria c = new Categoria();
        c.setId(3L);
        c.setNombre("Laptops");
        Producto p = new Producto();
        p.setId(10L);
        p.setNombre("Laptop X");
        p.setDescripcion("15 pulgadas");
        p.setPrecio(2500.0);
        p.setMarca("Acme");
        p.setStock(5);
        p.setImagenUrl("http://img/x.png");
        p.setCategoria(c);
        return p;
    }

    @Test
    void toDtoCopiaCamposYAplanaLaCategoria() {
        ProductoDTO dto = mapper.toDTO(producto());

        assertEquals(10L, dto.getId());
        assertEquals("Laptop X", dto.getNombre());
        assertEquals(2500.0, dto.getPrecio());
        assertEquals(5, dto.getStock());
        assertEquals(3L, dto.getCategoriaId());
        assertEquals("Laptops", dto.getCategoriaNombre());
    }

    @Test
    void toDtoSinCategoriaNoFalla() {
        Producto p = producto();
        p.setCategoria(null);

        ProductoDTO dto = mapper.toDTO(p);

        assertNull(dto.getCategoriaId());
        assertNull(dto.getCategoriaNombre());
    }

    @Test
    void toEntityArmaLaCategoriaSoloConElId() {
        ProductoDTO dto = mapper.toDTO(producto());

        Producto p = mapper.toEntity(dto);

        assertEquals(3L, p.getCategoria().getId());
        assertEquals("Laptop X", p.getNombre());
    }

    @Test
    void nullEntraNullSale() {
        assertNull(mapper.toDTO(null));
        assertNull(mapper.toEntity(null));
    }

    @Test
    void updateEntityFromDtoIgnoraLosCamposNulos() {
        Producto p = producto();
        ProductoDTO dto = new ProductoDTO();
        dto.setPrecio(1999.0);

        mapper.updateEntityFromDTO(dto, p);

        assertEquals(1999.0, p.getPrecio());
        assertEquals("Laptop X", p.getNombre());
        assertEquals(5, p.getStock());
        assertEquals(3L, p.getCategoria().getId());
    }
}
