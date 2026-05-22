package com.mysawit.pengiriman.usecase;

import com.mysawit.pengiriman.dto.DriverSummaryResponse;
import com.mysawit.pengiriman.dto.ShipmentQueryRequest;
import com.mysawit.pengiriman.dto.ShipmentResponse;
import java.util.List;
import java.util.UUID;

public interface ShipmentQueryUseCase {

    ShipmentResponse getShipmentById(UUID shipmentId);

    List<ShipmentResponse> getShipments(ShipmentQueryRequest request);

    List<ShipmentResponse> getShipmentsApprovedByMandor(ShipmentQueryRequest request);

    List<DriverSummaryResponse> getAvailableDriversForMandor(String driverId, String search);
}
