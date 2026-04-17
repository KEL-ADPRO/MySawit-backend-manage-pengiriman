package com.mysawit.mysawit_pengiriman.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifikasiRequest {
    @NotNull
    private Boolean approved;
    private String alasan;
    private Double angkutanDiakui;
}
