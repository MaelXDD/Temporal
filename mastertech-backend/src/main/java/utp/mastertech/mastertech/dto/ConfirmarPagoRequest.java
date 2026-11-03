package utp.mastertech.mastertech.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmarPagoRequest {
    @NotBlank(message = "El identificador de la sesión de pago es obligatorio")
    private String sessionId;
}
