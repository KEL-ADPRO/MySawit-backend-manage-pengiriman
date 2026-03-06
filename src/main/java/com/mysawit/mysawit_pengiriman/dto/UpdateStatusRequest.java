package com.mysawit.mysawit_pengiriman.dto;
import com.mysawit.mysawit_pengiriman.model.StatusPengiriman;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    private StatusPengiriman status;
}