package utp.mastertech.mastertech.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import utp.mastertech.mastertech.dto.VentaDTO;
import utp.mastertech.mastertech.exception.ResourceNotFoundException;
import utp.mastertech.mastertech.mapper.VentaMapper;
import utp.mastertech.mastertech.service.VentaService;

import java.util.List;
import java.util.stream.Collectors;

/** Reporte de ventas (solo ADMIN, ver SecurityConfig). */
@RestController
@RequestMapping("/api/v1/ventas")
public class VentaRestController {

    @Autowired
    private VentaService ventaService;

    @Autowired
    private VentaMapper ventaMapper;

    @GetMapping
    public List<VentaDTO> listar() {
        return ventaService.listarVentas().stream()
                .map(ventaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public VentaDTO obtener(@PathVariable Long id) {
        return ventaService.obtenerVenta(id)
                .map(ventaMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + id));
    }
}
