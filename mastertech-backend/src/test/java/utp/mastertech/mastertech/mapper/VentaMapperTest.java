package utp.mastertech.mastertech.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import utp.mastertech.mastertech.dto.VentaDTO;
import utp.mastertech.mastertech.model.DetalleVenta;
import utp.mastertech.mastertech.model.Usuario;
import utp.mastertech.mastertech.model.Venta;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VentaMapperTest {

    private VentaMapper mapper;

    @BeforeEach
    void preparar() {
        mapper = new VentaMapper();
        ReflectionTestUtils.setField(mapper, "detalleVentaMapper", new DetalleVentaMapper());
    }

    @Test
    void toDtoCopiaLaVentaElUsuarioYLosDetalles() {
        Usuario u = new Usuario();
        u.setId(2L);
        u.setNombre("Ana");
        u.setEmail("ana@x.com");
        DetalleVenta d = new DetalleVenta();
        d.setCantidad(2);
        Venta v = new Venta();
        v.setId(9L);
        v.setNumeroOrden("MT-ABC1234");
        v.setTotal(new BigDecimal("100.00"));
        v.setCantidadItems(2);
        v.setUsuario(u);
        v.setDetalles(List.of(d));

        VentaDTO dto = mapper.toDTO(v);

        assertEquals(9L, dto.getId());
        assertEquals("MT-ABC1234", dto.getNumeroOrden());
        assertEquals("Pagado", dto.getEstado());
        assertEquals(2L, dto.getUsuarioId());
        assertEquals("ana@x.com", dto.getUsuarioEmail());
        assertEquals(1, dto.getDetalles().size());
        assertEquals(2, dto.getDetalles().get(0).getCantidad());
    }

    @Test
    void sinDetallesDevuelveListaVacia() {
        Venta v = new Venta();
        v.setDetalles(null);

        VentaDTO dto = mapper.toDTO(v);

        assertNotNull(dto.getDetalles());
        assertTrue(dto.getDetalles().isEmpty());
    }

    @Test
    void sinUsuarioNoFalla() {
        VentaDTO dto = mapper.toDTO(new Venta());

        assertNull(dto.getUsuarioId());
    }

    @Test
    void nullEntraNullSale() {
        assertNull(mapper.toDTO(null));
    }
}
