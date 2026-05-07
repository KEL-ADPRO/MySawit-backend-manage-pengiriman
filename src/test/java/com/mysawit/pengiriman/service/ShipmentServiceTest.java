package com.mysawit.pengiriman.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysawit.pengiriman.dto.AdminReviewRequest;
import com.mysawit.pengiriman.dto.CreateShipmentRequest;
import com.mysawit.pengiriman.dto.DriverStatusUpdateRequest;
import com.mysawit.pengiriman.dto.MandorReviewRequest;
import com.mysawit.pengiriman.entity.Shipment;
import com.mysawit.pengiriman.enums.AdminReviewDecision;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.exception.AccessDeniedBusinessException;
import com.mysawit.pengiriman.exception.BusinessRuleViolationException;
import com.mysawit.pengiriman.integration.dto.HarvestSummary;
import com.mysawit.pengiriman.integration.gateway.HarvestGateway;
import com.mysawit.pengiriman.integration.gateway.PaymentGateway;
import com.mysawit.pengiriman.integration.gateway.UserGateway;
import com.mysawit.pengiriman.mapper.ShipmentMapper;
import com.mysawit.pengiriman.repository.ShipmentRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private UserGateway userGateway;

    @Mock
    private HarvestGateway harvestGateway;

    @Mock
    private PaymentGateway paymentGateway;

    private ShipmentService shipmentService;

    @BeforeEach
    void setUp() {
        shipmentService = new ShipmentService(
            shipmentRepository,
            new ShipmentMapper(),
            userGateway,
            harvestGateway,
            paymentGateway
        );

        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment shipment = invocation.getArgument(0);
            if (shipment.getId() == null) {
                shipment.setId(UUID.randomUUID());
            }
            return shipment;
        });
    }

    @Test
    void createShipmentShouldRejectWhenNoHarvestSelected() {
        CreateShipmentRequest request = new CreateShipmentRequest("mandor-1", "driver-1", List.of());

        assertThrows(BusinessRuleViolationException.class, () -> shipmentService.createShipment(request));
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void createShipmentShouldRejectWhenTotalWeightExceedsTruckCapacity() {
        CreateShipmentRequest request = new CreateShipmentRequest(
            "mandor-1",
            "driver-1",
            List.of("harvest-1", "harvest-2")
        );
        when(userGateway.areMandorAndDriverInSameEstate("mandor-1", "driver-1")).thenReturn(true);
        when(harvestGateway.getApprovedHarvests(request.harvestIds())).thenReturn(List.of(
            new HarvestSummary("harvest-1", new BigDecimal("250"), true),
            new HarvestSummary("harvest-2", new BigDecimal("200.01"), true)
        ));

        assertThrows(BusinessRuleViolationException.class, () -> shipmentService.createShipment(request));
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void updateDriverStatusShouldRejectInvalidStatusJump() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = buildShipment(shipmentId, ShipmentStatus.MEMUAT, new BigDecimal("150"));
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        DriverStatusUpdateRequest request = new DriverStatusUpdateRequest("driver-1", ShipmentStatus.TIBA_DI_TUJUAN);

        assertThrows(
            BusinessRuleViolationException.class,
            () -> shipmentService.updateDriverStatus(shipmentId, request)
        );
    }

    @Test
    void updateDriverStatusShouldRejectWhenShipmentBelongsToAnotherDriver() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = buildShipment(shipmentId, ShipmentStatus.MEMUAT, new BigDecimal("150"));
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        DriverStatusUpdateRequest request = new DriverStatusUpdateRequest("driver-2", ShipmentStatus.MENGIRIM);

        assertThrows(
            AccessDeniedBusinessException.class,
            () -> shipmentService.updateDriverStatus(shipmentId, request)
        );
    }

    @Test
    void mandorReviewShouldRejectBeforeShipmentArrives() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = buildShipment(shipmentId, ShipmentStatus.MENGIRIM, new BigDecimal("150"));
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        assertThrows(
            BusinessRuleViolationException.class,
            () -> shipmentService.reviewByMandor(shipmentId, new MandorReviewRequest("mandor-1", true, null))
        );
    }

    @Test
    void mandorReviewShouldApproveAndTriggerDriverPayroll() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = buildShipment(shipmentId, ShipmentStatus.TIBA_DI_TUJUAN, new BigDecimal("180"));
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        var response = shipmentService.reviewByMandor(
            shipmentId,
            new MandorReviewRequest("mandor-1", true, null)
        );

        assertEquals(ShipmentStatus.DISETUJUI_MANDOR, response.status());
        verify(paymentGateway).triggerDriverPayroll("driver-1", shipmentId, new BigDecimal("180"));
    }

    @Test
    void adminReviewShouldRejectWhenShipmentNotApprovedByMandor() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = buildShipment(shipmentId, ShipmentStatus.TIBA_DI_TUJUAN, new BigDecimal("200"));
        when(userGateway.isAdmin("admin-1")).thenReturn(true);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        assertThrows(
            BusinessRuleViolationException.class,
            () -> shipmentService.reviewByAdmin(
                shipmentId,
                new AdminReviewRequest("admin-1", AdminReviewDecision.APPROVE, null, null)
            )
        );
    }

    @Test
    void adminReviewShouldPartiallyRejectWithRecognizedWeight() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = buildShipment(shipmentId, ShipmentStatus.DISETUJUI_MANDOR, new BigDecimal("200"));
        when(userGateway.isAdmin("admin-1")).thenReturn(true);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        var response = shipmentService.reviewByAdmin(
            shipmentId,
            new AdminReviewRequest(
                "admin-1",
                AdminReviewDecision.PARTIAL_REJECT,
                new BigDecimal("125.5"),
                "Sebagian sawit kurang"
            )
        );

        assertEquals(ShipmentStatus.DITOLAK_PARSIAL_ADMIN, response.status());
        assertEquals(new BigDecimal("125.5"), response.recognizedWeightKg());
        verify(paymentGateway).triggerMandorPayroll("mandor-1", shipmentId, new BigDecimal("125.5"));
    }

    @Test
    void adminReviewShouldRejectRecognizedWeightAboveTotalWeight() {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = buildShipment(shipmentId, ShipmentStatus.DISETUJUI_MANDOR, new BigDecimal("200"));
        when(userGateway.isAdmin("admin-1")).thenReturn(true);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        assertThrows(
            BusinessRuleViolationException.class,
            () -> shipmentService.reviewByAdmin(
                shipmentId,
                new AdminReviewRequest(
                    "admin-1",
                    AdminReviewDecision.PARTIAL_REJECT,
                    new BigDecimal("220"),
                    "Berat diakui melebihi total"
                )
            )
        );
    }

    private Shipment buildShipment(UUID shipmentId, ShipmentStatus status, BigDecimal totalWeightKg) {
        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setDriverId("driver-1");
        shipment.setMandorId("mandor-1");
        shipment.setHarvestIds(new ArrayList<>(List.of("harvest-1")));
        shipment.setStatus(status);
        shipment.setTotalWeightKg(totalWeightKg);
        return shipment;
    }
}
