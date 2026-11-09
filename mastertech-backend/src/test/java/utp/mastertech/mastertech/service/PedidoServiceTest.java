package utp.mastertech.mastertech.service;

import com.stripe.exception.ApiConnectionException;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import utp.mastertech.mastertech.dto.ItemPedidoDTO;
import utp.mastertech.mastertech.exception.ConflictException;
import utp.mastertech.mastertech.exception.PagoException;
import utp.mastertech.mastertech.exception.ResourceNotFoundException;
import utp.mastertech.mastertech.exception.StockInsuficienteException;
import utp.mastertech.mastertech.model.Usuario;
import utp.mastertech.mastertech.model.Venta;
import utp.mastertech.mastertech.repository.ProductoRepository;
import utp.mastertech.mastertech.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Stripe se simula con mockStatic: no se hace ninguna llamada de red. */
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private VentaService ventaService;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PedidoService service;

    @BeforeEach
    void configurar() {
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5173/");
    }

    private ItemPedidoDTO item(long productoId, int cantidad) {
        ItemPedidoDTO i = new ItemPedidoDTO();
        i.setProductoId(productoId);
        i.setCantidad(cantidad);
        return i;
    }

    private Session sesionPagada(String email, String items) {
        Session s = mock(Session.class);
        when(s.getPaymentStatus()).thenReturn("paid");
        when(s.getMetadata()).thenReturn(Map.of("email", email, "items", items));
        return s;
    }

    // ---------- crearCheckout ----------

    @Test
    void crearCheckoutConStockInsuficienteLanzaExcepcionAntesDeLlamarAStripe() {
        List<ItemPedidoDTO> items = List.of(item(1, 5));
        when(ventaService.consolidar(items)).thenReturn(items);
        when(ventaService.validarStock(items)).thenReturn(List.of("Solo hay 1 unidades disponibles de \"Mouse\"."));

        assertThrows(StockInsuficienteException.class, () -> service.crearCheckout("ana@x.com", items));

        verifyNoInteractions(productoRepository);
    }

    @Test
    void crearCheckoutConDemasiadosProductosDistintosLanzaIllegalArgument() {
        List<ItemPedidoDTO> muchos = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            muchos.add(item(1_000_000L + i, 1));   // "1000000:1," ocupa 10 caracteres
        }
        when(ventaService.consolidar(muchos)).thenReturn(muchos);
        when(ventaService.validarStock(muchos)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.crearCheckout("ana@x.com", muchos));
    }

    // ---------- confirmar ----------

    @Test
    void confirmarFallaSiStripeNoResponde() {
        try (MockedStatic<Session> stripe = mockStatic(Session.class)) {
            stripe.when(() -> Session.retrieve("cs_1"))
                    .thenThrow(new ApiConnectionException("sin red", new RuntimeException()));

            assertThrows(PagoException.class, () -> service.confirmar("ana@x.com", "cs_1"));
        }
    }

    @Test
    void confirmarRechazaUnPagoNoCompletado() {
        Session pendiente = mock(Session.class);
        when(pendiente.getPaymentStatus()).thenReturn("unpaid");
        try (MockedStatic<Session> stripe = mockStatic(Session.class)) {
            stripe.when(() -> Session.retrieve("cs_1")).thenReturn(pendiente);

            ConflictException ex = assertThrows(ConflictException.class, () -> service.confirmar("ana@x.com", "cs_1"));

            assertTrue(ex.getMessage().contains("todavía no figura como completado"));
        }
        verifyNoInteractions(ventaService);
    }

    @Test
    void confirmarRechazaUnaSesionDeOtraCuenta() {
        Session ajena = sesionPagada("otro@x.com", "1:2");
        try (MockedStatic<Session> stripe = mockStatic(Session.class)) {
            stripe.when(() -> Session.retrieve("cs_1")).thenReturn(ajena);

            ConflictException ex = assertThrows(ConflictException.class, () -> service.confirmar("ana@x.com", "cs_1"));

            assertTrue(ex.getMessage().contains("otra cuenta"));
        }
        verifyNoInteractions(ventaService);
    }

    @Test
    void confirmarFallaSiElUsuarioNoExiste() {
        Session s = sesionPagada("ana@x.com", "1:2");
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.empty());
        try (MockedStatic<Session> stripe = mockStatic(Session.class)) {
            stripe.when(() -> Session.retrieve("cs_1")).thenReturn(s);

            assertThrows(ResourceNotFoundException.class, () -> service.confirmar("ana@x.com", "cs_1"));
        }
    }

    @Test
    void confirmarRegistraLaVentaConLosItemsDeLaMetadata() {
        Usuario usuario = new Usuario();
        Venta venta = new Venta();
        Session s = sesionPagada("ANA@x.com", "12:2,15:1");
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(usuario));
        when(ventaService.procesarVenta(anyList(), eq(usuario), anyString())).thenReturn(venta);

        try (MockedStatic<Session> stripe = mockStatic(Session.class)) {
            stripe.when(() -> Session.retrieve("cs_1")).thenReturn(s);

            Venta resultado = service.confirmar("ana@x.com", "cs_1");

            assertSame(venta, resultado);
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ItemPedidoDTO>> items = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> orden = ArgumentCaptor.forClass(String.class);
        verify(ventaService).procesarVenta(items.capture(), eq(usuario), orden.capture());

        assertEquals(2, items.getValue().size());
        assertEquals(12L, items.getValue().get(0).getProductoId());
        assertEquals(2, items.getValue().get(0).getCantidad());
        assertEquals(15L, items.getValue().get(1).getProductoId());
        assertEquals(1, items.getValue().get(1).getCantidad());
        assertTrue(orden.getValue().matches("MT-[0-9A-Z]{7}"), "número de orden: " + orden.getValue());
        assertEquals(10, orden.getValue().length());
    }

    @Test
    void elNumeroDeOrdenEsDeterministaPorSesion() {
        Usuario usuario = new Usuario();
        Session s = sesionPagada("ana@x.com", "1:1");
        when(usuarioRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(usuario));
        when(ventaService.procesarVenta(anyList(), eq(usuario), anyString())).thenReturn(new Venta());

        try (MockedStatic<Session> stripe = mockStatic(Session.class)) {
            stripe.when(() -> Session.retrieve("cs_misma")).thenReturn(s);
            service.confirmar("ana@x.com", "cs_misma");
            service.confirmar("ana@x.com", "cs_misma");
        }

        ArgumentCaptor<String> orden = ArgumentCaptor.forClass(String.class);
        verify(ventaService, times(2)).procesarVenta(anyList(), eq(usuario), orden.capture());
        assertEquals(orden.getAllValues().get(0), orden.getAllValues().get(1));
    }
}
