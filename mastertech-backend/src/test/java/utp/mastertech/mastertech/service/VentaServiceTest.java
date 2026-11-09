package utp.mastertech.mastertech.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utp.mastertech.mastertech.dto.ItemPedidoDTO;
import utp.mastertech.mastertech.exception.StockInsuficienteException;
import utp.mastertech.mastertech.model.Producto;
import utp.mastertech.mastertech.model.Usuario;
import utp.mastertech.mastertech.model.Venta;
import utp.mastertech.mastertech.repository.DetalleVentaRepository;
import utp.mastertech.mastertech.repository.ProductoRepository;
import utp.mastertech.mastertech.repository.VentaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private DetalleVentaRepository detalleVentaRepository;
    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private VentaService service;

    private ItemPedidoDTO item(long productoId, int cantidad) {
        ItemPedidoDTO i = new ItemPedidoDTO();
        i.setProductoId(productoId);
        i.setCantidad(cantidad);
        return i;
    }

    private Producto producto(long id, String nombre, double precio, Integer stock) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setStock(stock);
        return p;
    }

    // ---------- consolidar ----------

    @Test
    void consolidarSumaLasLineasRepetidasYConservaElOrden() {
        List<ItemPedidoDTO> r = service.consolidar(List.of(item(1, 2), item(2, 1), item(1, 3)));

        assertEquals(2, r.size());
        assertEquals(1L, r.get(0).getProductoId());
        assertEquals(5, r.get(0).getCantidad());
        assertEquals(2L, r.get(1).getProductoId());
        assertEquals(1, r.get(1).getCantidad());
    }

    // ---------- validarStock ----------

    @Test
    void validarStockSinProblemasDevuelveListaVacia() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1, "Mouse", 50, 10)));

        assertTrue(service.validarStock(List.of(item(1, 10))).isEmpty());
    }

    @Test
    void validarStockReportaProductoInexistente() {
        when(productoRepository.findById(1L)).thenReturn(Optional.empty());

        List<String> errores = service.validarStock(List.of(item(1, 1)));

        assertEquals(1, errores.size());
        assertTrue(errores.get(0).contains("ya no está disponible"));
    }

    @Test
    void validarStockReportaCantidadMayorAlStock() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1, "Mouse", 50, 2)));

        List<String> errores = service.validarStock(List.of(item(1, 5)));

        assertEquals(1, errores.size());
        assertTrue(errores.get(0).contains("Solo hay 2 unidades"));
        assertTrue(errores.get(0).contains("Mouse"));
    }

    @Test
    void validarStockTrataStockNuloComoCero() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1, "Mouse", 50, null)));

        assertEquals(1, service.validarStock(List.of(item(1, 1))).size());
    }

    // ---------- procesarVenta ----------

    @Test
    void procesarVentaDescuentaStockYCalculaTotales() {
        Producto mouse = producto(1, "Mouse", 25.5, 10);
        Producto teclado = producto(2, "Teclado", 100.0, 4);
        when(ventaRepository.findByNumeroOrden("MT-ABC1234")).thenReturn(Optional.empty());
        when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mouse));
        when(productoRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(teclado));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));
        Usuario usuario = new Usuario();

        Venta v = service.procesarVenta(List.of(item(1, 2), item(2, 1)), usuario, "MT-ABC1234");

        assertEquals(8, mouse.getStock());
        assertEquals(3, teclado.getStock());
        assertEquals(0, new BigDecimal("151.00").compareTo(v.getTotal()));
        assertEquals(3, v.getCantidadItems());
        assertEquals("MT-ABC1234", v.getNumeroOrden());
        assertSame(usuario, v.getUsuario());
        assertEquals(2, v.getDetalles().size());
        assertNotNull(v.getFecha());
        verify(productoRepository).save(mouse);
        verify(productoRepository).save(teclado);
    }

    @Test
    void procesarVentaEsIdempotentePorNumeroDeOrden() {
        Venta existente = new Venta();
        existente.setNumeroOrden("MT-ABC1234");
        when(ventaRepository.findByNumeroOrden("MT-ABC1234")).thenReturn(Optional.of(existente));

        Venta v = service.procesarVenta(List.of(item(1, 2)), new Usuario(), "MT-ABC1234");

        assertSame(existente, v);
        verifyNoInteractions(productoRepository);
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void procesarVentaSinStockLanzaExcepcionYNoGuardaLaVenta() {
        when(ventaRepository.findByNumeroOrden("MT-ABC1234")).thenReturn(Optional.empty());
        when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(producto(1, "Mouse", 50, 1)));

        StockInsuficienteException ex = assertThrows(StockInsuficienteException.class,
                () -> service.procesarVenta(List.of(item(1, 3)), new Usuario(), "MT-ABC1234"));

        assertTrue(ex.getMessage().contains("Solo hay 1 unidades"));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void procesarVentaConProductoInexistenteFalla() {
        when(ventaRepository.findByNumeroOrden("MT-ABC1234")).thenReturn(Optional.empty());
        when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThrows(StockInsuficienteException.class,
                () -> service.procesarVenta(List.of(item(1, 1)), new Usuario(), "MT-ABC1234"));

        verify(ventaRepository, never()).save(any());
    }

    // ---------- consultas ----------

    @Test
    void listarYObtenerDelegaEnElRepositorio() {
        Venta v = new Venta();
        when(ventaRepository.findAll()).thenReturn(List.of(v));
        when(ventaRepository.findById(3L)).thenReturn(Optional.of(v));
        when(ventaRepository.count()).thenReturn(7L);

        assertEquals(1, service.listarVentas().size());
        assertTrue(service.obtenerVenta(3L).isPresent());
        assertEquals(7L, service.contarVentas());
    }
}
