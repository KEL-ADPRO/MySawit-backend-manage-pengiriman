package com.mysawit.pengiriman.dto;

import com.mysawit.pengiriman.enums.ShipmentStatus;
import java.time.LocalDate;

public record ShipmentQueryRequest(
    String driverId,
    String mandorId,
    ShipmentStatus status,
    LocalDate date
) {
}
