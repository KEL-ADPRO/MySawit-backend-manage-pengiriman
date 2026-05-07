package com.mysawit.pengiriman.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateShipmentRequest(
    @NotBlank String mandorId,
    @NotBlank String driverId,
    @NotEmpty @Size(max = 20) List<@NotBlank String> harvestIds
) {
}
