package utp.mastertech.mastertech.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import utp.mastertech.mastertech.exception.ApiExceptionHandler;
import utp.mastertech.mastertech.mapper.CategoriaMapper;
import utp.mastertech.mastertech.model.Categoria;
import utp.mastertech.mastertech.service.ProductoService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoriaRestControllerTest {

    @Mock
    private ProductoService productoService;

    @Spy
    private CategoriaMapper categoriaMapper = new CategoriaMapper();

    @InjectMocks
    private CategoriaRestController controller;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private Categoria categoria(Long id, String nombre) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setNombre(nombre);
        return c;
    }

    @Test
    void listarDevuelveLasCategorias() throws Exception {
        when(productoService.listarCategorias()).thenReturn(List.of(categoria(1L, "Audio"), categoria(2L, "Laptops")));

        String cuerpo = mvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("Audio"));
        assertTrue(cuerpo.contains("Laptops"));
    }

    @Test
    void obtenerInexistenteDevuelve404() throws Exception {
        when(productoService.obtenerCategoria(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/categorias/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearDevuelve201() throws Exception {
        mvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Monitores\",\"descripcion\":\"Pantallas\"}"))
                .andExpect(status().isCreated());

        verify(productoService).guardarCategoria(any(Categoria.class));
    }

    @Test
    void crearSinNombreDevuelve400() throws Exception {
        mvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(productoService, never()).guardarCategoria(any(Categoria.class));
    }

    @Test
    void eliminarCategoriaConProductosDevuelve409() throws Exception {
        when(productoService.obtenerCategoria(1L)).thenReturn(Optional.of(categoria(1L, "Audio")));
        when(productoService.categoriaEstaVacia(1L)).thenReturn(false);

        mvc.perform(delete("/api/v1/categorias/1"))
                .andExpect(status().isConflict());

        verify(productoService, never()).eliminarCategoria(1L);
    }

    @Test
    void eliminarCategoriaVaciaDevuelve204() throws Exception {
        when(productoService.obtenerCategoria(1L)).thenReturn(Optional.of(categoria(1L, "Audio")));
        when(productoService.categoriaEstaVacia(1L)).thenReturn(true);

        mvc.perform(delete("/api/v1/categorias/1"))
                .andExpect(status().isNoContent());

        verify(productoService).eliminarCategoria(1L);
    }

    @Test
    void eliminarInexistenteDevuelve404() throws Exception {
        when(productoService.obtenerCategoria(5L)).thenReturn(Optional.empty());

        mvc.perform(delete("/api/v1/categorias/5"))
                .andExpect(status().isNotFound());
    }
}
