package com.mysawit.pengiriman.dto;

import com.mysawit.pengiriman.enums.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DriverStatusUpdateRequest(
    @NotBlank String driverId,
    @NotNull ShipmentStatus newStatus
) {
}
