package com.mysawit.pengiriman.service.strategy;

import com.mysawit.pengiriman.dto.AdminReviewRequest;
import com.mysawit.pengiriman.entity.Shipment;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RejectAdminStrategy implements AdminReviewStrategy {

    @Override
    public void execute(Shipment shipment, AdminReviewRequest request) {
        if (!StringUtils.hasText(request.rejectionReason())) {
            throw new BusinessRuleViolationException("Admin rejection reason is required");
        }
        shipment.setStatus(ShipmentStatus.DITOLAK_ADMIN);
        shipment.setRecognizedWeightKg(java.math.BigDecimal.ZERO);
        shipment.setRejectionReason(request.rejectionReason().trim());
    }
}
