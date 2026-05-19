package com.mysawit.pengiriman.service.strategy;

import com.mysawit.pengiriman.dto.AdminReviewRequest;
import com.mysawit.pengiriman.entity.Shipment;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import org.springframework.stereotype.Component;

@Component
public class ApproveAdminStrategy implements AdminReviewStrategy {

    @Override
    public void execute(Shipment shipment, AdminReviewRequest request) {
        shipment.setStatus(ShipmentStatus.DISETUJUI_ADMIN);
        shipment.setRecognizedWeightKg(shipment.getTotalWeightKg());
        shipment.setRejectionReason(null);
    }
}
