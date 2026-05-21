package com.mysawit.pengiriman.dto;

import com.mysawit.pengiriman.enums.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShipmentResponse(
    UUID id,
    String driverId,
    String mandorId,
    List<String> harvestIds,
    BigDecimal totalWeightKg,
    BigDecimal recognizedWeightKg,
    ShipmentStatus status,
    String rejectionReason,
    Instant createdAt,
    Instant updatedAt,
    Instant mandorReviewedAt,
    Instant adminReviewedAt
) {
}
