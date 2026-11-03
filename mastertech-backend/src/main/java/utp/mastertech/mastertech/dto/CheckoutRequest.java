package utp.mastertech.mastertech.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequest {
    @Valid
    @NotEmpty(message = "El carrito está vacío")
    private List<ItemPedidoDTO> items;
}
