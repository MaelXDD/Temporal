package utp.mastertech.mastertech.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import utp.mastertech.mastertech.dto.CheckoutRequest;
import utp.mastertech.mastertech.dto.ConfirmarPagoRequest;
import utp.mastertech.mastertech.dto.VentaDTO;
import utp.mastertech.mastertech.mapper.VentaMapper;
import utp.mastertech.mastertech.service.PedidoService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoRestController {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private VentaMapper ventaMapper;

    /** Crea la sesión de Stripe Checkout y devuelve su URL. */
    @PostMapping("/checkout")
    public Map<String, String> checkout(@Valid @RequestBody CheckoutRequest request,
                                        Authentication authentication) {
        String url = pedidoService.crearCheckout(authentication.getName(), request.getItems());
        return Map.of("url", url);
    }

    /** Se llama al volver de Stripe: verifica el pago y registra la venta. */
    @PostMapping("/confirmar")
    public VentaDTO confirmar(@Valid @RequestBody ConfirmarPagoRequest request,
                              Authentication authentication) {
        return ventaMapper.toDTO(pedidoService.confirmar(authentication.getName(), request.getSessionId()));
    }
}
