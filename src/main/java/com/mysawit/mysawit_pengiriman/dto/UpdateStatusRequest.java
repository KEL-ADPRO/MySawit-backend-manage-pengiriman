package com.mysawit.mysawit_pengiriman.dto;
import com.mysawit.mysawit_pengiriman.model.StatusPengiriman;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotNull
    private StatusPengiriman status;
}
