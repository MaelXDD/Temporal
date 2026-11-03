package utp.mastertech.mastertech.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CambiarPasswordRequest {
    @NotBlank(message = "Ingresa tu contraseña actual")
    private String passwordActual;

    @NotBlank(message = "Ingresa la nueva contraseña")
    private String passwordNuevo;

    @NotBlank(message = "Confirma la nueva contraseña")
    private String passwordConfirm;
}
