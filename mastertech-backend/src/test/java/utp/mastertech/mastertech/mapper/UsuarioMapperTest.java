package utp.mastertech.mastertech.mapper;

import org.junit.jupiter.api.Test;
import utp.mastertech.mastertech.dto.UsuarioDTO;
import utp.mastertech.mastertech.model.Usuario;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioMapperTest {

    private final UsuarioMapper mapper = new UsuarioMapper();

    @Test
    void toDtoCopiaLosCamposYNoExponeLaPassword() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombre("Ana");
        u.setEmail("ana@x.com");
        u.setPassword("HASH");
        u.setRol("ADMIN");
        u.setDni("12345678");

        UsuarioDTO dto = mapper.toDTO(u);

        assertEquals(1L, dto.getId());
        assertEquals("Ana", dto.getNombre());
        assertEquals("ADMIN", dto.getRol());
        assertNull(dto.getPassword());
    }

    @Test
    void toDtoYToEntityDevuelvenNullSiLaEntradaEsNull() {
        assertNull(mapper.toDTO(null));
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toEntityConservaElRolPorDefectoSiElDtoNoTraeRol() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setNombre("Ana");
        dto.setEmail("ana@x.com");

        Usuario u = mapper.toEntity(dto);

        assertEquals("USER", u.getRol());
    }

    @Test
    void updateEntityFromDtoSoloCambiaLoQueVieneInformado() {
        Usuario u = new Usuario();
        u.setNombre("Ana");
        u.setEmail("ana@x.com");
        u.setDireccion("Calle 1");
        UsuarioDTO dto = new UsuarioDTO();
        dto.setNombre("Ana Maria");

        mapper.updateEntityFromDTO(dto, u);

        assertEquals("Ana Maria", u.getNombre());
        assertEquals("ana@x.com", u.getEmail());
        assertEquals("Calle 1", u.getDireccion());
    }
}
