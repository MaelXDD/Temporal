package utp.mastertech.mastertech.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import utp.mastertech.mastertech.dto.VentaDTO;
import utp.mastertech.mastertech.exception.ApiExceptionHandler;
import utp.mastertech.mastertech.mapper.VentaMapper;
import utp.mastertech.mastertech.model.Venta;
import utp.mastertech.mastertech.service.VentaService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VentaRestControllerTest {

    @Mock
    private VentaService ventaService;
    @Mock
    private VentaMapper ventaMapper;

    @InjectMocks
    private VentaRestController controller;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private VentaDTO dto(String orden) {
        VentaDTO d = new VentaDTO();
        d.setNumeroOrden(orden);
        return d;
    }

    @Test
    void listarDevuelveLasVentasMapeadas() throws Exception {
        Venta v = new Venta();
        when(ventaService.listarVentas()).thenReturn(List.of(v));
        when(ventaMapper.toDTO(v)).thenReturn(dto("MT-AAA1111"));

        String cuerpo = mvc.perform(get("/api/v1/ventas"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("MT-AAA1111"));
    }

    @Test
    void obtenerExistenteDevuelve200() throws Exception {
        Venta v = new Venta();
        when(ventaService.obtenerVenta(3L)).thenReturn(Optional.of(v));
        when(ventaMapper.toDTO(v)).thenReturn(dto("MT-BBB2222"));

        mvc.perform(get("/api/v1/ventas/3"))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerInexistenteDevuelve404() throws Exception {
        when(ventaService.obtenerVenta(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/ventas/99"))
                .andExpect(status().isNotFound());
    }
}
