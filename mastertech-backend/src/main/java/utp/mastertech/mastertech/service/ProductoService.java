package utp.mastertech.mastertech.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import utp.mastertech.mastertech.model.Categoria;
import utp.mastertech.mastertech.model.Producto;
import utp.mastertech.mastertech.repository.CategoriaRepository;
import utp.mastertech.mastertech.repository.ProductoRepository;
import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private SupabaseStorageService storageService;

    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    public List<Producto> buscarProductos(String keyword) {
        if (keyword == null || keyword.isBlank()) return listarProductos();
        return productoRepository.buscarPorNombre(keyword);
    }

    public Optional<Producto> obtenerProducto(Long id) {
        return productoRepository.findById(id);
    }

    /** Guardado sin imagen (lo usa la API REST: conserva la imagenUrl que traiga el producto). */
    public void guardarProducto(Producto producto) {
        productoRepository.save(producto);
    }

    /**
     * Guarda el producto y, si llega una imagen, la sube al bucket y escribe la URL
     * resultante en la tabla productos.
     *
     * Orden de operaciones (sin @Transactional a propósito: cada paso confirma por separado
     * para poder compensar):
     *  1. Se sube la imagen nueva al bucket.
     *  2. Se guarda el producto con la URL nueva.
     *     - Si falla el guardado -> se borra la imagen recién subida (compensación).
     *  3. Solo con el guardado confirmado se borra la imagen anterior del bucket.
     */
    public void guardarProducto(Producto producto, MultipartFile imagen) {
        String urlAnterior = null;
        if (producto.getId() != null) {
            // La URL vigente se toma de la BD, no del formulario.
            urlAnterior = productoRepository.findById(producto.getId())
                    .map(Producto::getImagenUrl)
                    .orElse(null);
            producto.setImagenUrl(urlAnterior);
        } else {
            producto.setImagenUrl(null);
        }

        String urlNueva = null;
        if (imagen != null && !imagen.isEmpty()) {
            urlNueva = storageService.subirImagenProducto(imagen);
            producto.setImagenUrl(urlNueva);
        }

        try {
            productoRepository.save(producto);
        } catch (RuntimeException e) {
            if (urlNueva != null) {
                storageService.eliminarPorUrl(urlNueva);
            }
            throw e;
        }

        if (urlNueva != null && urlAnterior != null) {
            storageService.eliminarPorUrl(urlAnterior);
        }
    }

    /**
     * Elimina el producto y, una vez borrada la fila, su imagen del bucket.
     * Si el borrado falla (p. ej. el producto tiene ventas asociadas) la imagen no se toca.
     */
    public void eliminarProducto(Long id) {
        String url = productoRepository.findById(id)
                .map(Producto::getImagenUrl)
                .orElse(null);
        productoRepository.deleteById(id);
        storageService.eliminarPorUrl(url);
    }

    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }

    public Optional<Categoria> obtenerCategoria(Long id) {
        return categoriaRepository.findById(id);
    }

    public void guardarCategoria(Categoria categoria) {
        categoriaRepository.save(categoria);
    }

    public List<Producto> obtenerStockBajo(Integer limite) {
        return productoRepository.findByStockLessThanEqual(limite);
    }

    public boolean categoriaEstaVacia(Long id) {
        return productoRepository.findByCategoriaId(id).isEmpty();
    }

    /**
     * Sube (o reemplaza) la imagen de la categoría. Mismo orden que en productos:
     * sube la nueva, guarda la URL y solo entonces borra la anterior del bucket
     * (si el guardado falla, se borra la recién subida).
     */
    public void guardarCategoria(Categoria categoria, MultipartFile imagen) {
        String urlAnterior = categoriaRepository.findById(categoria.getId())
                .map(Categoria::getImagenUrl)
                .orElse(null);
        categoria.setImagenUrl(urlAnterior);

        if (imagen == null || imagen.isEmpty()) {
            return;
        }
        String urlNueva = storageService.subirImagenCategoria(imagen);
        categoria.setImagenUrl(urlNueva);
        try {
            categoriaRepository.save(categoria);
        } catch (RuntimeException e) {
            storageService.eliminarPorUrl(urlNueva);
            throw e;
        }
        if (urlAnterior != null) {
            storageService.eliminarPorUrl(urlAnterior);
        }
    }

    /** Quita la imagen de la categoría: primero limpia la columna y luego borra el archivo del bucket. */
    public void quitarImagenCategoria(Categoria categoria) {
        String url = categoria.getImagenUrl();
        categoria.setImagenUrl(null);
        categoriaRepository.save(categoria);
        storageService.eliminarPorUrl(url);
    }

    /** Elimina la categoría y, una vez borrada la fila, su imagen del bucket. */
    public void eliminarCategoria(Long id) {
        String url = categoriaRepository.findById(id)
                .map(Categoria::getImagenUrl)
                .orElse(null);
        categoriaRepository.deleteById(id);
        storageService.eliminarPorUrl(url);
    }
}