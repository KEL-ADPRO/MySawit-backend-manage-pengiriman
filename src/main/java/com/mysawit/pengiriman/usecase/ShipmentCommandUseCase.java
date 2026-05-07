package com.mysawit.pengiriman.usecase;

import com.mysawit.pengiriman.dto.AdminReviewRequest;
import com.mysawit.pengiriman.dto.CreateShipmentRequest;
import com.mysawit.pengiriman.dto.DriverStatusUpdateRequest;
import com.mysawit.pengiriman.dto.MandorReviewRequest;
import com.mysawit.pengiriman.dto.ShipmentResponse;
import java.util.UUID;

public interface ShipmentCommandUseCase {

    ShipmentResponse createShipment(CreateShipmentRequest request);

    ShipmentResponse updateDriverStatus(UUID shipmentId, DriverStatusUpdateRequest request);

    ShipmentResponse reviewByMandor(UUID shipmentId, MandorReviewRequest request);

    ShipmentResponse reviewByAdmin(UUID shipmentId, AdminReviewRequest request);
}
