package utp.mastertech.mastertech.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import utp.mastertech.mastertech.dto.VentaDTO;
import utp.mastertech.mastertech.exception.ApiExceptionHandler;
import utp.mastertech.mastertech.exception.ConflictException;
import utp.mastertech.mastertech.exception.StockInsuficienteException;
import utp.mastertech.mastertech.mapper.VentaMapper;
import utp.mastertech.mastertech.model.Venta;
import utp.mastertech.mastertech.service.PedidoService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PedidoRestControllerTest {

    @Mock
    private PedidoService pedidoService;
    @Mock
    private VentaMapper ventaMapper;

    @InjectMocks
    private PedidoRestController controller;

    private MockMvc mvc;
    private final UsuarioPrincipal ana = new UsuarioPrincipal();

    /** Autenticación de prueba: el controlador solo usa authentication.getName(). */
    private static class UsuarioPrincipal extends UsernamePasswordAuthenticationToken {
        UsuarioPrincipal() {
            super("ana@x.com", null);
        }
    }

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void checkoutDevuelveLaUrlDeStripe() throws Exception {
        when(pedidoService.crearCheckout(eq("ana@x.com"), anyList())).thenReturn("https://stripe.test/pay/abc");

        String cuerpo = mvc.perform(post("/api/v1/pedidos/checkout")
                        .principal(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productoId\":1,\"cantidad\":2}]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("https://stripe.test/pay/abc"));
    }

    @Test
    void checkoutConCarritoVacioDevuelve400() throws Exception {
        mvc.perform(post("/api/v1/pedidos/checkout")
                        .principal(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(pedidoService);
    }

    @Test
    void checkoutConCantidadCeroDevuelve400() throws Exception {
        mvc.perform(post("/api/v1/pedidos/checkout")
                        .principal(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productoId\":1,\"cantidad\":0}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkoutSinStockDevuelve409() throws Exception {
        when(pedidoService.crearCheckout(eq("ana@x.com"), anyList()))
                .thenThrow(new StockInsuficienteException(List.of("Solo hay 1 unidades disponibles de \"Mouse\".")));

        mvc.perform(post("/api/v1/pedidos/checkout")
                        .principal(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productoId\":1,\"cantidad\":5}]}"))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmarDevuelveLaVentaRegistrada() throws Exception {
        Venta venta = new Venta();
        VentaDTO dto = new VentaDTO();
        dto.setNumeroOrden("MT-ABC1234");
        when(pedidoService.confirmar("ana@x.com", "cs_1")).thenReturn(venta);
        when(ventaMapper.toDTO(venta)).thenReturn(dto);

        String cuerpo = mvc.perform(post("/api/v1/pedidos/confirmar")
                        .principal(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"cs_1\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("MT-ABC1234"));
    }

    @Test
    void confirmarSinSessionIdDevuelve400() throws Exception {
        mvc.perform(post("/api/v1/pedidos/confirmar")
                        .principal(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(pedidoService);
    }

    @Test
    void confirmarPagoPendienteDevuelve409() throws Exception {
        when(pedidoService.confirmar("ana@x.com", "cs_1"))
                .thenThrow(new ConflictException("El pago todavía no figura como completado."));

        mvc.perform(post("/api/v1/pedidos/confirmar")
                        .principal(ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"cs_1\"}"))
                .andExpect(status().isConflict());
    }
}
