package utp.mastertech.mastertech.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utp.mastertech.mastertech.dto.ItemPedidoDTO;
import utp.mastertech.mastertech.exception.StockInsuficienteException;
import utp.mastertech.mastertech.model.DetalleVenta;
import utp.mastertech.mastertech.model.Producto;
import utp.mastertech.mastertech.model.Usuario;
import utp.mastertech.mastertech.model.Venta;
import utp.mastertech.mastertech.repository.DetalleVentaRepository;
import utp.mastertech.mastertech.repository.ProductoRepository;
import utp.mastertech.mastertech.repository.VentaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    /** Une líneas repetidas del mismo producto sumando cantidades (conserva el orden). */
    public List<ItemPedidoDTO> consolidar(List<ItemPedidoDTO> items) {
        Map<Long, Integer> porProducto = new LinkedHashMap<>();
        for (ItemPedidoDTO item : items) {
            porProducto.merge(item.getProductoId(), item.getCantidad(), Integer::sum);
        }
        List<ItemPedidoDTO> resultado = new ArrayList<>();
        porProducto.forEach((id, cantidad) -> {
            ItemPedidoDTO dto = new ItemPedidoDTO();
            dto.setProductoId(id);
            dto.setCantidad(cantidad);
            resultado.add(dto);
        });
        return resultado;
    }

    /** Devuelve un mensaje por cada producto inexistente o sin stock suficiente. */
    public List<String> validarStock(List<ItemPedidoDTO> items) {
        List<String> errores = new ArrayList<>();
        for (ItemPedidoDTO item : items) {
            Optional<Producto> opt = productoRepository.findById(item.getProductoId());
            if (opt.isEmpty()) {
                errores.add("Uno de los productos del carrito ya no está disponible.");
                continue;
            }
            Producto p = opt.get();
            int stockActual = p.getStock() != null ? p.getStock() : 0;
            if (item.getCantidad() > stockActual) {
                errores.add("Solo hay " + stockActual + " unidades disponibles de \"" + p.getNombre() + "\".");
            }
        }
        return errores;
    }

    /**
     * Registra la venta y descuenta el stock. Es idempotente por numeroOrden: si ya existe
     * una venta con ese número (p. ej. el usuario recarga la página de confirmación),
     * devuelve la existente sin volver a descontar stock.
     */
    @Transactional
    public Venta procesarVenta(List<ItemPedidoDTO> itemsEntrada, Usuario usuario, String numeroOrden) {
        Optional<Venta> existente = ventaRepository.findByNumeroOrden(numeroOrden);
        if (existente.isPresent()) {
            return existente.get();
        }

        List<ItemPedidoDTO> items = consolidar(itemsEntrada);
        List<String> errores = new ArrayList<>();
        List<DetalleVenta> detalles = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int cantidadItems = 0;

        Venta venta = new Venta();
        for (ItemPedidoDTO item : items) {
            Optional<Producto> opt = productoRepository.findByIdForUpdate(item.getProductoId());
            if (opt.isEmpty()) {
                errores.add("Uno de los productos del carrito ya no está disponible.");
                continue;
            }
            Producto p = opt.get();
            int stockActual = p.getStock() != null ? p.getStock() : 0;
            if (item.getCantidad() > stockActual) {
                errores.add("Solo hay " + stockActual + " unidades disponibles de \"" + p.getNombre() + "\".");
                continue;
            }
            p.setStock(stockActual - item.getCantidad());
            productoRepository.save(p);

            BigDecimal precio = BigDecimal.valueOf(p.getPrecio());
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(item.getCantidad()));

            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setProducto(p);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(precio);
            detalle.setSubtotal(subtotal);
            detalles.add(detalle);

            total = total.add(subtotal);
            cantidadItems += item.getCantidad();
        }

        if (!errores.isEmpty()) {
            // La excepción revierte la transacción: no se descuenta stock de ningún producto.
            throw new StockInsuficienteException(errores);
        }

        venta.setFecha(LocalDateTime.now());
        venta.setTotal(total);
        venta.setCantidadItems(cantidadItems);
        venta.setNumeroOrden(numeroOrden);
        venta.setUsuario(usuario);
        venta.setDetalles(detalles);
        return ventaRepository.save(venta);
    }

    public List<Venta> listarVentas() {
        return ventaRepository.findAll();
    }

    public Optional<Venta> obtenerVenta(Long id) {
        return ventaRepository.findById(id);
    }

    public List<DetalleVenta> obtenerDetallesPorVenta(Long ventaId) {
        return detalleVentaRepository.findByVentaId(ventaId);
    }

    public long contarVentas() {
        return ventaRepository.count();
    }
}
