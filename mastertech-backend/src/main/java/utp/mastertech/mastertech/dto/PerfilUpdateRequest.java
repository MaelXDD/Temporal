package utp.mastertech.mastertech.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PerfilUpdateRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Size(max = 255, message = "La dirección no puede exceder los 255 caracteres")
    private String direccion;

    @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener 9 dígitos")
    private String numeroTelefono;
}
