package utp.mastertech.mastertech.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PerfilDTO {
    private Long id;
    private String nombre;
    private String email;
    private String rol;
    private String dni;
    private String direccion;
    private String numeroTelefono;
    private LocalDateTime fechaRegistro;
}
