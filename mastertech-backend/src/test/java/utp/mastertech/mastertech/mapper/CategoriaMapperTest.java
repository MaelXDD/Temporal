package utp.mastertech.mastertech.mapper;

import org.junit.jupiter.api.Test;
import utp.mastertech.mastertech.dto.CategoriaDTO;
import utp.mastertech.mastertech.model.Categoria;

import static org.junit.jupiter.api.Assertions.*;

class CategoriaMapperTest {

    private final CategoriaMapper mapper = new CategoriaMapper();

    @Test
    void toDtoIncluyeLaUrlDeLaImagen() {
        Categoria c = new Categoria();
        c.setId(1L);
        c.setNombre("Audio");
        c.setImagenUrl("http://img/audio.png");

        CategoriaDTO dto = mapper.toDTO(c);

        assertEquals("Audio", dto.getNombre());
        assertEquals("http://img/audio.png", dto.getImagenUrl());
    }

    @Test
    void toEntityNoCopiaLaUrlDeLaImagen() {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setNombre("Audio");
        dto.setImagenUrl("http://malicioso/x.png");

        Categoria c = mapper.toEntity(dto);

        assertEquals("Audio", c.getNombre());
        assertNull(c.getImagenUrl());
    }

    @Test
    void updateEntityFromDtoSoloCambiaNombreYDescripcion() {
        Categoria c = new Categoria();
        c.setNombre("Viejo");
        c.setDescripcion("desc");
        c.setImagenUrl("http://img/a.png");
        CategoriaDTO dto = new CategoriaDTO();
        dto.setNombre("Nuevo");
        dto.setImagenUrl("http://otra/b.png");

        mapper.updateEntityFromDTO(dto, c);

        assertEquals("Nuevo", c.getNombre());
        assertEquals("desc", c.getDescripcion());
        assertEquals("http://img/a.png", c.getImagenUrl());
    }

    @Test
    void nullEntraNullSale() {
        assertNull(mapper.toDTO(null));
        assertNull(mapper.toEntity(null));
    }
}
