package com.mysawit.pengiriman.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Observer Pattern: domain event fired when admin fully approves a shipment.
 * Listener triggers async mandor payroll for full weight.
 */
public record ShipmentApprovedByAdminEvent(
    String mandorId,
    UUID shipmentId,
    BigDecimal recognizedWeightKg
) {
}
