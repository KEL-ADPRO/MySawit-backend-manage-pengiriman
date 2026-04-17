package com.mysawit.mysawit_pengiriman.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AssignPengirimanRequest {
    @NotNull
    private UUID supirId;

    @NotNull
    private UUID mandorId;

    @NotBlank
    private String supirNama;

    @NotBlank
    private String mandorNama;

    @NotNull
    @Positive
    private Double totalAngkutan;

    @NotEmpty
    private List<UUID> hasilPanenIds;
}
