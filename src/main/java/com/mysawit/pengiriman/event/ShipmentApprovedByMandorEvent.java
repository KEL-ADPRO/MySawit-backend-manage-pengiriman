package com.mysawit.pengiriman.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Observer Pattern: domain event fired when mandor approves a shipment.
 * Listener triggers async driver payroll.
 */
public record ShipmentApprovedByMandorEvent(
    String driverId,
    UUID shipmentId,
    BigDecimal totalWeightKg
) {
}
