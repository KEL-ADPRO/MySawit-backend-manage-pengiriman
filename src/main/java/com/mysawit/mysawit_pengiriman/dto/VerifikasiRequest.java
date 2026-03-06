package com.mysawit.mysawit_pengiriman.dto;
import lombok.Data;

@Data
public class VerifikasiRequest {
    private boolean approved;
    private String alasan;
    private Double angkutanDiakui;
}