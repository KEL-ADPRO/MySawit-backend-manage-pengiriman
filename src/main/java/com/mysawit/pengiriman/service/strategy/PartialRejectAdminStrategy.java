package com.mysawit.pengiriman.service.strategy;

import com.mysawit.pengiriman.dto.AdminReviewRequest;
import com.mysawit.pengiriman.entity.Shipment;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.exception.BusinessRuleViolationException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PartialRejectAdminStrategy implements AdminReviewStrategy {

    @Override
    public void execute(Shipment shipment, AdminReviewRequest request) {
        if (!StringUtils.hasText(request.rejectionReason())) {
            throw new BusinessRuleViolationException("Admin rejection reason is required");
        }
        if (request.recognizedWeightKg() == null) {
            throw new BusinessRuleViolationException(
                "Recognized weight is required for partial rejection"
            );
        }

        BigDecimal recognizedWeight = request.recognizedWeightKg();
        if (recognizedWeight.compareTo(BigDecimal.ZERO) <= 0
            || recognizedWeight.compareTo(shipment.getTotalWeightKg()) >= 0) {
            throw new BusinessRuleViolationException(
                "Recognized weight must be greater than 0 and lower than total shipment weight"
            );
        }

        shipment.setStatus(ShipmentStatus.DITOLAK_PARSIAL_ADMIN);
        shipment.setRecognizedWeightKg(recognizedWeight);
        shipment.setRejectionReason(request.rejectionReason().trim());
    }
}
