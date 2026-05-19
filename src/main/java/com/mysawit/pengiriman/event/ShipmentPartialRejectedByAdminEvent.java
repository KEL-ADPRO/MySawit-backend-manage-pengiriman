package com.mysawit.pengiriman.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Observer Pattern: domain event fired when admin partially rejects a shipment.
 * Listener triggers async mandor payroll for the recognized portion only.
 */
public record ShipmentPartialRejectedByAdminEvent(
    String mandorId,
    UUID shipmentId,
    BigDecimal recognizedWeightKg
) {
}
