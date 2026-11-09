package utp.mastertech.mastertech.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import utp.mastertech.mastertech.model.Categoria;
import utp.mastertech.mastertech.model.Producto;
import utp.mastertech.mastertech.repository.CategoriaRepository;
import utp.mastertech.mastertech.repository.ProductoRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private SupabaseStorageService storageService;

    @InjectMocks
    private ProductoService service;

    private final MockMultipartFile imagen =
            new MockMultipartFile("imagen", "foto.png", "image/png", new byte[]{1, 2, 3});

    // ---------- búsqueda ----------

    @Test
    void buscarSinPalabraDevuelveTodos() {
        when(productoRepository.findAll()).thenReturn(List.of(new Producto(), new Producto()));

        assertEquals(2, service.buscarProductos("  ").size());
        assertEquals(2, service.buscarProductos(null).size());
        verify(productoRepository, never()).buscarPorNombre(any());
    }

    @Test
    void buscarConPalabraUsaLaConsultaPorNombre() {
        when(productoRepository.buscarPorNombre("mouse")).thenReturn(List.of(new Producto()));

        assertEquals(1, service.buscarProductos("mouse").size());
    }

    // ---------- guardar producto con imagen ----------

    @Test
    void productoNuevoConImagenSubeYGuardaLaUrl() {
        Producto p = new Producto();
        when(storageService.subirImagenProducto(imagen)).thenReturn("http://bucket/nueva.png");

        service.guardarProducto(p, imagen);

        assertEquals("http://bucket/nueva.png", p.getImagenUrl());
        verify(productoRepository).save(p);
        verify(storageService, never()).eliminarPorUrl(any());
    }

    @Test
    void productoNuevoSinImagenQuedaSinUrl() {
        Producto p = new Producto();
        p.setImagenUrl("http://del-formulario/x.png");

        service.guardarProducto(p, null);

        assertNull(p.getImagenUrl());
        verify(storageService, never()).subirImagenProducto(any());
        verify(productoRepository).save(p);
    }

    @Test
    void alReemplazarLaImagenBorraLaAnteriorDespuesDeGuardar() {
        Producto existente = new Producto();
        existente.setId(5L);
        existente.setImagenUrl("http://bucket/vieja.png");
        Producto editado = new Producto();
        editado.setId(5L);
        when(productoRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(storageService.subirImagenProducto(imagen)).thenReturn("http://bucket/nueva.png");

        service.guardarProducto(editado, imagen);

        InOrder orden = inOrder(storageService, productoRepository);
        orden.verify(storageService).subirImagenProducto(imagen);
        orden.verify(productoRepository).save(editado);
        orden.verify(storageService).eliminarPorUrl("http://bucket/vieja.png");
        assertEquals("http://bucket/nueva.png", editado.getImagenUrl());
    }

    @Test
    void siFallaElGuardadoBorraLaImagenRecienSubidaYConservaLaVieja() {
        Producto existente = new Producto();
        existente.setId(5L);
        existente.setImagenUrl("http://bucket/vieja.png");
        Producto editado = new Producto();
        editado.setId(5L);
        when(productoRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(storageService.subirImagenProducto(imagen)).thenReturn("http://bucket/nueva.png");
        doThrow(new RuntimeException("BD caída")).when(productoRepository).save(editado);

        assertThrows(RuntimeException.class, () -> service.guardarProducto(editado, imagen));

        verify(storageService).eliminarPorUrl("http://bucket/nueva.png");
        verify(storageService, never()).eliminarPorUrl("http://bucket/vieja.png");
    }

    @Test
    void editarSinImagenNuevaConservaLaUrlDeLaBaseDeDatos() {
        Producto existente = new Producto();
        existente.setId(5L);
        existente.setImagenUrl("http://bucket/actual.png");
        Producto editado = new Producto();
        editado.setId(5L);
        when(productoRepository.findById(5L)).thenReturn(Optional.of(existente));

        service.guardarProducto(editado, null);

        assertEquals("http://bucket/actual.png", editado.getImagenUrl());
        verify(storageService, never()).eliminarPorUrl(any());
    }

    // ---------- eliminar ----------

    @Test
    void eliminarProductoBorraLaFilaYLuegoLaImagen() {
        Producto p = new Producto();
        p.setId(8L);
        p.setImagenUrl("http://bucket/p8.png");
        when(productoRepository.findById(8L)).thenReturn(Optional.of(p));

        service.eliminarProducto(8L);

        InOrder orden = inOrder(productoRepository, storageService);
        orden.verify(productoRepository).deleteById(8L);
        orden.verify(storageService).eliminarPorUrl("http://bucket/p8.png");
    }

    @Test
    void siNoSePuedeBorrarElProductoLaImagenNoSeToca() {
        Producto p = new Producto();
        p.setImagenUrl("http://bucket/p8.png");
        when(productoRepository.findById(8L)).thenReturn(Optional.of(p));
        doThrow(new RuntimeException("tiene ventas")).when(productoRepository).deleteById(8L);

        assertThrows(RuntimeException.class, () -> service.eliminarProducto(8L));

        verifyNoInteractions(storageService);
    }

    // ---------- categorías ----------

    @Test
    void categoriaVaciaSiNoTieneProductos() {
        when(productoRepository.findByCategoriaId(1L)).thenReturn(List.of());
        when(productoRepository.findByCategoriaId(2L)).thenReturn(List.of(new Producto()));

        assertTrue(service.categoriaEstaVacia(1L));
        assertFalse(service.categoriaEstaVacia(2L));
    }

    @Test
    void quitarImagenDeCategoriaLimpiaLaColumnaYBorraElArchivo() {
        Categoria c = new Categoria();
        c.setImagenUrl("http://bucket/cat.png");

        service.quitarImagenCategoria(c);

        assertNull(c.getImagenUrl());
        InOrder orden = inOrder(categoriaRepository, storageService);
        orden.verify(categoriaRepository).save(c);
        orden.verify(storageService).eliminarPorUrl("http://bucket/cat.png");
    }

    @Test
    void reemplazarImagenDeCategoriaBorraLaAnterior() {
        Categoria existente = new Categoria();
        existente.setId(2L);
        existente.setImagenUrl("http://bucket/vieja-cat.png");
        Categoria editada = new Categoria();
        editada.setId(2L);
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(existente));
        when(storageService.subirImagenCategoria(imagen)).thenReturn("http://bucket/nueva-cat.png");

        service.guardarCategoria(editada, imagen);

        assertEquals("http://bucket/nueva-cat.png", editada.getImagenUrl());
        verify(categoriaRepository).save(editada);
        verify(storageService).eliminarPorUrl("http://bucket/vieja-cat.png");
    }

    @Test
    void eliminarCategoriaBorraLaFilaYSuImagen() {
        Categoria c = new Categoria();
        c.setImagenUrl("http://bucket/cat.png");
        when(categoriaRepository.findById(4L)).thenReturn(Optional.of(c));

        service.eliminarCategoria(4L);

        verify(categoriaRepository).deleteById(4L);
        verify(storageService).eliminarPorUrl("http://bucket/cat.png");
    }
}
