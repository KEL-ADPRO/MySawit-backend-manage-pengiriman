package com.mysawit.pengiriman.service.strategy;

import com.mysawit.pengiriman.dto.AdminReviewRequest;
import com.mysawit.pengiriman.entity.Shipment;

/**
 * Strategy Pattern interface for admin review decisions.
 */
public interface AdminReviewStrategy {

    void execute(Shipment shipment, AdminReviewRequest request);
}
