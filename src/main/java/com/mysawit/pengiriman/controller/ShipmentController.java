package com.mysawit.pengiriman.controller;

import com.mysawit.pengiriman.dto.AdminReviewRequest;
import com.mysawit.pengiriman.dto.CreateShipmentRequest;
import com.mysawit.pengiriman.dto.DriverSummaryResponse;
import com.mysawit.pengiriman.dto.DriverStatusUpdateRequest;
import com.mysawit.pengiriman.dto.MandorReviewRequest;
import com.mysawit.pengiriman.dto.ShipmentQueryRequest;
import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.usecase.ShipmentCommandUseCase;
import com.mysawit.pengiriman.usecase.ShipmentQueryUseCase;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    private final ShipmentCommandUseCase shipmentCommandUseCase;
    private final ShipmentQueryUseCase shipmentQueryUseCase;

    public ShipmentController(
        ShipmentCommandUseCase shipmentCommandUseCase,
        ShipmentQueryUseCase shipmentQueryUseCase
    ) {
        this.shipmentCommandUseCase = shipmentCommandUseCase;
        this.shipmentQueryUseCase = shipmentQueryUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse createShipment(@Valid @RequestBody CreateShipmentRequest request) {
        return shipmentCommandUseCase.createShipment(request);
    }

    @GetMapping("/{shipmentId}")
    public ShipmentResponse getShipmentById(@PathVariable UUID shipmentId) {
        return shipmentQueryUseCase.getShipmentById(shipmentId);
    }

    @GetMapping
    public List<ShipmentResponse> getShipments(
        @RequestParam(required = false) String driverId,
        @RequestParam(required = false) String mandorId,
        @RequestParam(required = false) ShipmentStatus status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return shipmentQueryUseCase.getShipments(new ShipmentQueryRequest(driverId, mandorId, status, date));
    }

    @GetMapping("/approved-by-mandor")
    public List<ShipmentResponse> getShipmentsApprovedByMandor(
        @RequestParam(required = false) String mandorId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return shipmentQueryUseCase.getShipmentsApprovedByMandor(
            new ShipmentQueryRequest(null, mandorId, ShipmentStatus.DISETUJUI_MANDOR, date)
        );
    }

    @GetMapping("/drivers")
    public List<DriverSummaryResponse> getDriversForMandor(
        @RequestParam String mandorId,
        @RequestParam(required = false) String search
    ) {
        return shipmentQueryUseCase.getAvailableDriversForMandor(mandorId, search);
    }

    @PatchMapping("/{shipmentId}/driver-status")
    public ShipmentResponse updateDriverStatus(
        @PathVariable UUID shipmentId,
        @Valid @RequestBody DriverStatusUpdateRequest request
    ) {
        return shipmentCommandUseCase.updateDriverStatus(shipmentId, request);
    }

    @PatchMapping("/{shipmentId}/mandor-review")
    public ShipmentResponse reviewByMandor(
        @PathVariable UUID shipmentId,
        @Valid @RequestBody MandorReviewRequest request
    ) {
        return shipmentCommandUseCase.reviewByMandor(shipmentId, request);
    }

    @PatchMapping("/{shipmentId}/admin-review")
    public ShipmentResponse reviewByAdmin(
        @PathVariable UUID shipmentId,
        @Valid @RequestBody AdminReviewRequest request
    ) {
        return shipmentCommandUseCase.reviewByAdmin(shipmentId, request);
    }
}
