package com.mysawit.pengiriman.mapper;

import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.entity.Shipment;
import org.springframework.stereotype.Component;

@Component
public class ShipmentMapper {

    public ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(
            shipment.getId(),
            shipment.getDriverId(),
            shipment.getMandorId(),
            shipment.getHarvestIds(),
            shipment.getTotalWeightKg(),
            shipment.getRecognizedWeightKg(),
            shipment.getStatus(),
            shipment.getRejectionReason(),
            shipment.getCreatedAt(),
            shipment.getUpdatedAt(),
            shipment.getMandorReviewedAt(),
            shipment.getAdminReviewedAt()
        );
    }
}
