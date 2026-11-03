package utp.mastertech.mastertech.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import utp.mastertech.mastertech.dto.ItemPedidoDTO;
import utp.mastertech.mastertech.exception.ConflictException;
import utp.mastertech.mastertech.exception.PagoException;
import utp.mastertech.mastertech.exception.ResourceNotFoundException;
import utp.mastertech.mastertech.exception.StockInsuficienteException;
import utp.mastertech.mastertech.model.Producto;
import utp.mastertech.mastertech.model.Usuario;
import utp.mastertech.mastertech.model.Venta;
import utp.mastertech.mastertech.repository.ProductoRepository;
import utp.mastertech.mastertech.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checkout sin sesión de servidor: el front envía las líneas del carrito (id + cantidad),
 * el backend toma precios y stock de la BD, crea la sesión de Stripe y guarda las líneas
 * en la metadata de esa sesión. Al volver del pago, /confirmar lee la metadata y registra la venta.
 */
@Service
public class PedidoService {

    private static final int MAX_METADATA = 500; // límite de Stripe por valor de metadata

    @Autowired
    private VentaService ventaService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    /** @return URL de Stripe Checkout a la que el front debe redirigir al cliente. */
    public String crearCheckout(String email, List<ItemPedidoDTO> itemsEntrada) {
        List<ItemPedidoDTO> items = ventaService.consolidar(itemsEntrada);

        List<String> errores = ventaService.validarStock(items);
        if (!errores.isEmpty()) {
            throw new StockInsuficienteException(errores);
        }

        String metadataItems = codificarItems(items);
        if (metadataItems.length() > MAX_METADATA) {
            throw new IllegalArgumentException("El carrito tiene demasiados productos distintos para pagar de una vez.");
        }

        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        for (ItemPedidoDTO item : items) {
            Producto p = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + item.getProductoId()));
            long centimos = BigDecimal.valueOf(p.getPrecio())
                    .movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
            lineItems.add(SessionCreateParams.LineItem.builder()
                    .setQuantity((long) item.getCantidad())
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("pen")
                            .setUnitAmount(centimos)
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(p.getNombre())
                                    .build())
                            .build())
                    .build());
        }

        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(base + "/pago/confirmar?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(base + "/carrito")
                    .putMetadata("email", email)
                    .putMetadata("items", metadataItems)
                    .addAllLineItem(lineItems)
                    .build();
            return Session.create(params).getUrl();
        } catch (StripeException e) {
            throw new PagoException("Error al conectar con Stripe: " + e.getMessage(), e);
        }
    }

    /** Verifica el pago en Stripe y registra la venta (idempotente). */
    public Venta confirmar(String email, String sessionId) {
        Session session;
        try {
            session = Session.retrieve(sessionId);
        } catch (StripeException e) {
            throw new PagoException("No se pudo verificar el pago con Stripe.", e);
        }

        if (!"paid".equals(session.getPaymentStatus())) {
            throw new ConflictException("El pago todavía no figura como completado.");
        }

        Map<String, String> metadata = session.getMetadata();
        if (metadata == null || !email.equalsIgnoreCase(metadata.get("email"))) {
            throw new ConflictException("Esta sesión de pago pertenece a otra cuenta.");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));

        List<ItemPedidoDTO> items = decodificarItems(metadata.get("items"));
        return ventaService.procesarVenta(items, usuario, numeroOrdenDe(sessionId));
    }

    // "12:2,15:1" <-> [{productoId:12,cantidad:2},{productoId:15,cantidad:1}]
    private String codificarItems(List<ItemPedidoDTO> items) {
        StringBuilder sb = new StringBuilder();
        for (ItemPedidoDTO i : items) {
            if (sb.length() > 0) sb.append(',');
            sb.append(i.getProductoId()).append(':').append(i.getCantidad());
        }
        return sb.toString();
    }

    private List<ItemPedidoDTO> decodificarItems(String texto) {
        List<ItemPedidoDTO> items = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            throw new ConflictException("La sesión de pago no contiene productos.");
        }
        for (String par : texto.split(",")) {
            String[] partes = par.split(":");
            ItemPedidoDTO dto = new ItemPedidoDTO();
            dto.setProductoId(Long.parseLong(partes[0]));
            dto.setCantidad(Integer.parseInt(partes[1]));
            items.add(dto);
        }
        return items;
    }

    /** Número de orden determinista (MT- + 7 caracteres) derivado de la sesión de Stripe: 10 caracteres en total. */
    private String numeroOrdenDe(String sessionId) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(sessionId.getBytes(StandardCharsets.UTF_8));
            String base36 = new BigInteger(1, hash).toString(36).toUpperCase();
            return "MT-" + base36.substring(0, 7);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
