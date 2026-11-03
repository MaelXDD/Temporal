package utp.mastertech.mastertech.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import utp.mastertech.mastertech.dto.CategoriaDTO;
import utp.mastertech.mastertech.exception.ConflictException;
import utp.mastertech.mastertech.exception.ResourceNotFoundException;
import utp.mastertech.mastertech.mapper.CategoriaMapper;
import utp.mastertech.mastertech.model.Categoria;
import utp.mastertech.mastertech.service.ProductoService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaRestController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CategoriaMapper categoriaMapper;

    @GetMapping
    public List<CategoriaDTO> listar() {
        return productoService.listarCategorias().stream()
                .map(categoriaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public CategoriaDTO obtener(@PathVariable Long id) {
        return productoService.obtenerCategoria(id)
                .map(categoriaMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
    }

    @PostMapping
    public ResponseEntity<CategoriaDTO> crear(@Valid @RequestBody CategoriaDTO dto) {
        dto.setId(null);
        Categoria categoria = categoriaMapper.toEntity(dto);
        productoService.guardarCategoria(categoria);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaMapper.toDTO(categoria));
    }

    @PutMapping("/{id}")
    public CategoriaDTO actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaDTO dto) {
        Categoria categoria = productoService.obtenerCategoria(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
        categoriaMapper.updateEntityFromDTO(dto, categoria);
        productoService.guardarCategoria(categoria);
        return categoriaMapper.toDTO(categoria);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        Categoria categoria = productoService.obtenerCategoria(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
        if (!productoService.categoriaEstaVacia(id)) {
            throw new ConflictException("No se puede eliminar \"" + categoria.getNombre()
                    + "\": tiene productos asociados.");
        }
        productoService.eliminarCategoria(id);
        return ResponseEntity.noContent().build();
    }
}
