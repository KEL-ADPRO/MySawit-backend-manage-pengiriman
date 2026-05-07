package com.mysawit.pengiriman.repository;

import com.mysawit.pengiriman.entity.Shipment;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.data.jpa.domain.Specification;

public final class ShipmentSpecifications {

    private ShipmentSpecifications() {
    }

    public static Specification<Shipment> hasDriverId(String driverId) {
        return (root, query, cb) -> driverId == null ? null : cb.equal(root.get("driverId"), driverId);
    }

    public static Specification<Shipment> hasMandorId(String mandorId) {
        return (root, query, cb) -> mandorId == null ? null : cb.equal(root.get("mandorId"), mandorId);
    }

    public static Specification<Shipment> hasStatus(ShipmentStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Shipment> createdOn(LocalDate date) {
        if (date == null) {
            return null;
        }
        return (root, query, cb) -> cb.between(
            root.get("createdAt"),
            date.atStartOfDay().toInstant(ZoneOffset.UTC),
            date.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC)
        );
    }
}
