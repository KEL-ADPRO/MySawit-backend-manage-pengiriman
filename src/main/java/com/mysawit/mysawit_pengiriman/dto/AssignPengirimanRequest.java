package com.mysawit.mysawit_pengiriman.dto;
import lombok.Data;
import java.util.UUID;

@Data
public class AssignPengirimanRequest {
    private UUID supirId;
    private UUID mandorId;
    private Double totalAngkutan;
}