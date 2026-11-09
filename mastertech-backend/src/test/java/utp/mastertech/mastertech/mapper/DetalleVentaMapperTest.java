package utp.mastertech.mastertech.mapper;

import org.junit.jupiter.api.Test;
import utp.mastertech.mastertech.dto.DetalleVentaDTO;
import utp.mastertech.mastertech.model.DetalleVenta;
import utp.mastertech.mastertech.model.Producto;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DetalleVentaMapperTest {

    private final DetalleVentaMapper mapper = new DetalleVentaMapper();

    @Test
    void toDtoCopiaCantidadesYDatosDelProducto() {
        Producto p = new Producto();
        p.setId(4L);
        p.setNombre("Mouse");
        p.setImagenUrl("http://img/mouse.png");
        DetalleVenta d = new DetalleVenta();
        d.setId(1L);
        d.setCantidad(3);
        d.setPrecioUnitario(new BigDecimal("50.00"));
        d.setSubtotal(new BigDecimal("150.00"));
        d.setProducto(p);

        DetalleVentaDTO dto = mapper.toDTO(d);

        assertEquals(3, dto.getCantidad());
        assertEquals(new BigDecimal("150.00"), dto.getSubtotal());
        assertEquals(4L, dto.getProductoId());
        assertEquals("Mouse", dto.getProductoNombre());
        assertEquals("http://img/mouse.png", dto.getProductoImagenUrl());
    }

    @Test
    void toDtoSinProductoNoFalla() {
        DetalleVentaDTO dto = mapper.toDTO(new DetalleVenta());

        assertNull(dto.getProductoId());
    }

    @Test
    void nullEntraNullSale() {
        assertNull(mapper.toDTO(null));
    }
}
