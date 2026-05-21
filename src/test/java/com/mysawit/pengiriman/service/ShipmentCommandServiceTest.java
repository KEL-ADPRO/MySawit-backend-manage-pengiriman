package com.mysawit.pengiriman.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysawit.pengiriman.dto.AdminReviewRequest;
import com.mysawit.pengiriman.dto.CreateShipmentRequest;
import com.mysawit.pengiriman.dto.DriverStatusUpdateRequest;
import com.mysawit.pengiriman.dto.MandorReviewRequest;
import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.entity.Shipment;
import com.mysawit.pengiriman.enums.AdminReviewDecision;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.event.ShipmentApprovedByAdminEvent;
import com.mysawit.pengiriman.event.ShipmentApprovedByMandorEvent;
import com.mysawit.pengiriman.event.ShipmentPartialRejectedByAdminEvent;
import com.mysawit.pengiriman.exception.AccessDeniedBusinessException;
import com.mysawit.pengiriman.exception.BusinessRuleViolationException;
import com.mysawit.pengiriman.exception.ResourceNotFoundException;
import com.mysawit.pengiriman.integration.dto.HarvestSummary;
import com.mysawit.pengiriman.integration.gateway.HarvestGateway;
import com.mysawit.pengiriman.integration.gateway.UserGateway;
import com.mysawit.pengiriman.mapper.ShipmentMapper;
import com.mysawit.pengiriman.repository.ShipmentRepository;
import com.mysawit.pengiriman.service.strategy.AdminReviewStrategyFactory;
import com.mysawit.pengiriman.service.strategy.ApproveAdminStrategy;
import com.mysawit.pengiriman.service.strategy.PartialRejectAdminStrategy;
import com.mysawit.pengiriman.service.strategy.RejectAdminStrategy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ShipmentCommandServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private UserGateway userGateway;
    @Mock
    private HarvestGateway harvestGateway;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ShipmentCommandService service;

    @BeforeEach
    void setUp() {
        AdminReviewStrategyFactory factory = new AdminReviewStrategyFactory(
            new ApproveAdminStrategy(),
            new RejectAdminStrategy(),
            new PartialRejectAdminStrategy()
        );
        service = new ShipmentCommandService(
            shipmentRepository,
            new ShipmentMapper(),
            userGateway,
            harvestGateway,
            factory,
            eventPublisher
        );

        lenient().when(shipmentRepository.save(any(Shipment.class)))
            .thenAnswer(inv -> {
                Shipment s = inv.getArgument(0);
                if (s.getId() == null) {
                    s.setId(UUID.randomUUID());
                }
                return s;
            });
    }

    // ─── Create Shipment ────────────────────────────────────────

    @Nested
    @DisplayName("createShipment")
    class CreateShipment {

        @Test
        @DisplayName("should reject when no harvests are selected")
        void rejectEmptyHarvests() {
            CreateShipmentRequest req =
                new CreateShipmentRequest("m1", "d1", List.of());

            assertThrows(BusinessRuleViolationException.class,
                () -> service.createShipment(req));
            verify(shipmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject when mandor and driver are not in same estate")
        void rejectDifferentEstate() {
            CreateShipmentRequest req =
                new CreateShipmentRequest("m1", "d1", List.of("h1"));
            when(userGateway.areMandorAndDriverInSameEstate("m1", "d1"))
                .thenReturn(false);

            assertThrows(AccessDeniedBusinessException.class,
                () -> service.createShipment(req));
        }

        @Test
        @DisplayName("should reject when total weight exceeds 400 kg")
        void rejectOverweight() {
            CreateShipmentRequest req =
                new CreateShipmentRequest("m1", "d1", List.of("h1", "h2"));
            when(userGateway.areMandorAndDriverInSameEstate("m1", "d1"))
                .thenReturn(true);
            when(harvestGateway.getApprovedHarvests(req.harvestIds()))
                .thenReturn(List.of(
                    new HarvestSummary("h1", new BigDecimal("250"), true),
                    new HarvestSummary("h2", new BigDecimal("200.01"), true)
                ));

            assertThrows(BusinessRuleViolationException.class,
                () -> service.createShipment(req));
        }

        @Test
        @DisplayName("should reject when some harvests are not approved")
        void rejectUnapprovedHarvests() {
            CreateShipmentRequest req =
                new CreateShipmentRequest("m1", "d1", List.of("h1"));
            when(userGateway.areMandorAndDriverInSameEstate("m1", "d1"))
                .thenReturn(true);
            when(harvestGateway.getApprovedHarvests(req.harvestIds()))
                .thenReturn(List.of(
                    new HarvestSummary("h1", new BigDecimal("100"), false)
                ));

            assertThrows(BusinessRuleViolationException.class,
                () -> service.createShipment(req));
        }

        @Test
        @DisplayName("should reject when harvest ids are invalid")
        void rejectInvalidHarvestIds() {
            CreateShipmentRequest req =
                new CreateShipmentRequest("m1", "d1", List.of("h1", "h2"));
            when(userGateway.areMandorAndDriverInSameEstate("m1", "d1"))
                .thenReturn(true);
            when(harvestGateway.getApprovedHarvests(req.harvestIds()))
                .thenReturn(List.of(
                    new HarvestSummary("h1", new BigDecimal("100"), true)
                ));

            assertThrows(BusinessRuleViolationException.class,
                () -> service.createShipment(req));
        }

        @Test
        @DisplayName("should create shipment successfully")
        void createSuccess() {
            CreateShipmentRequest req =
                new CreateShipmentRequest("m1", "d1", List.of("h1", "h2"));
            when(userGateway.areMandorAndDriverInSameEstate("m1", "d1"))
                .thenReturn(true);
            when(harvestGateway.getApprovedHarvests(req.harvestIds()))
                .thenReturn(List.of(
                    new HarvestSummary("h1", new BigDecimal("100"), true),
                    new HarvestSummary("h2", new BigDecimal("120"), true)
                ));

            ShipmentResponse resp = service.createShipment(req);

            assertNotNull(resp.id());
            assertEquals("d1", resp.driverId());
            assertEquals("m1", resp.mandorId());
            assertEquals(ShipmentStatus.MEMUAT, resp.status());
            assertEquals(new BigDecimal("220"), resp.totalWeightKg());
        }
    }

    // ─── Update Driver Status ───────────────────────────────────

    @Nested
    @DisplayName("updateDriverStatus")
    class UpdateDriverStatus {

        @Test
        @DisplayName("should reject when driver is not assigned")
        void rejectWrongDriver() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.MEMUAT, "150");
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            assertThrows(AccessDeniedBusinessException.class,
                () -> service.updateDriverStatus(id,
                    new DriverStatusUpdateRequest("other-driver", ShipmentStatus.MENGIRIM)));
        }

        @Test
        @DisplayName("should reject invalid status jump MEMUAT -> TIBA")
        void rejectInvalidTransition() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.MEMUAT, "150");
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            assertThrows(BusinessRuleViolationException.class,
                () -> service.updateDriverStatus(id,
                    new DriverStatusUpdateRequest("driver-1", ShipmentStatus.TIBA_DI_TUJUAN)));
        }

        @Test
        @DisplayName("should reject when shipment is in terminal status")
        void rejectTerminalStatus() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.DISETUJUI_MANDOR, "150");
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            assertThrows(BusinessRuleViolationException.class,
                () -> service.updateDriverStatus(id,
                    new DriverStatusUpdateRequest("driver-1", ShipmentStatus.MENGIRIM)));
        }

        @Test
        @DisplayName("should update MEMUAT -> MENGIRIM successfully")
        void updateMemuatToMengirim() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.MEMUAT, "150");
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            ShipmentResponse resp = service.updateDriverStatus(id,
                new DriverStatusUpdateRequest("driver-1", ShipmentStatus.MENGIRIM));

            assertEquals(ShipmentStatus.MENGIRIM, resp.status());
        }

        @Test
        @DisplayName("should update MENGIRIM -> TIBA_DI_TUJUAN successfully")
        void updateMengirimToTiba() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.MENGIRIM, "150");
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            ShipmentResponse resp = service.updateDriverStatus(id,
                new DriverStatusUpdateRequest("driver-1", ShipmentStatus.TIBA_DI_TUJUAN));

            assertEquals(ShipmentStatus.TIBA_DI_TUJUAN, resp.status());
        }

        @Test
        @DisplayName("should throw ResourceNotFound when shipment does not exist")
        void rejectMissingShipment() {
            UUID id = UUID.randomUUID();
            when(shipmentRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                () -> service.updateDriverStatus(id,
                    new DriverStatusUpdateRequest("driver-1", ShipmentStatus.MENGIRIM)));
        }
    }

    // ─── Review by Mandor ───────────────────────────────────────

    @Nested
    @DisplayName("reviewByMandor")
    class ReviewByMandor {

        @Test
        @DisplayName("should reject when mandor is not assigned")
        void rejectWrongMandor() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.TIBA_DI_TUJUAN, "180");
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            assertThrows(AccessDeniedBusinessException.class,
                () -> service.reviewByMandor(id,
                    new MandorReviewRequest("other-mandor", true, null)));
        }

        @Test
        @DisplayName("should reject review before shipment arrives")
        void rejectBeforeArrival() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.MENGIRIM, "180");
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            assertThrows(BusinessRuleViolationException.class,
                () -> service.reviewByMandor(id,
                    new MandorReviewRequest("mandor-1", true, null)));
        }

        @Test
        @DisplayName("should approve and publish driver payroll event")
        void approveSuccess() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.TIBA_DI_TUJUAN, "180");
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            ShipmentResponse resp = service.reviewByMandor(id,
                new MandorReviewRequest("mandor-1", true, null));

            assertEquals(ShipmentStatus.DISETUJUI_MANDOR, resp.status());
            assertNull(resp.rejectionReason());
            assertNotNull(resp.mandorReviewedAt());

            ArgumentCaptor<ShipmentApprovedByMandorEvent> captor =
                ArgumentCaptor.forClass(ShipmentApprovedByMandorEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertEquals("driver-1", captor.getValue().driverId());
            assertEquals(new BigDecimal("180"), captor.getValue().totalWeightKg());
        }

        @Test
        @DisplayName("should reject with reason and not publish event")
        void rejectWithReason() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.TIBA_DI_TUJUAN, "180");
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            ShipmentResponse resp = service.reviewByMandor(id,
                new MandorReviewRequest("mandor-1", false, "Berat kurang"));

            assertEquals(ShipmentStatus.DITOLAK_MANDOR, resp.status());
            assertEquals("Berat kurang", resp.rejectionReason());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should reject when rejection reason is missing")
        void rejectMissingReason() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.TIBA_DI_TUJUAN, "180");
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            assertThrows(BusinessRuleViolationException.class,
                () -> service.reviewByMandor(id,
                    new MandorReviewRequest("mandor-1", false, null)));
        }

        @Test
        @DisplayName("should reject when rejection reason is blank")
        void rejectBlankReason() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.TIBA_DI_TUJUAN, "180");
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            assertThrows(BusinessRuleViolationException.class,
                () -> service.reviewByMandor(id,
                    new MandorReviewRequest("mandor-1", false, "  ")));
        }
    }

    // ─── Review by Admin ────────────────────────────────────────

    @Nested
    @DisplayName("reviewByAdmin")
    class ReviewByAdmin {

        @Test
        @DisplayName("should reject when user is not admin")
        void rejectNonAdmin() {
            UUID id = UUID.randomUUID();
            when(userGateway.isAdmin("user-1")).thenReturn(false);

            assertThrows(AccessDeniedBusinessException.class,
                () -> service.reviewByAdmin(id,
                    new AdminReviewRequest("user-1", AdminReviewDecision.APPROVE, null, null)));
        }

        @Test
        @DisplayName("should reject when shipment not approved by mandor")
        void rejectWrongStatus() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.TIBA_DI_TUJUAN, "200");
            when(userGateway.isAdmin("admin-1")).thenReturn(true);
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            assertThrows(BusinessRuleViolationException.class,
                () -> service.reviewByAdmin(id,
                    new AdminReviewRequest("admin-1", AdminReviewDecision.APPROVE, null, null)));
        }

        @Test
        @DisplayName("should approve and publish admin approved event")
        void approveSuccess() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.DISETUJUI_MANDOR, "200");
            when(userGateway.isAdmin("admin-1")).thenReturn(true);
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            ShipmentResponse resp = service.reviewByAdmin(id,
                new AdminReviewRequest("admin-1", AdminReviewDecision.APPROVE, null, null));

            assertEquals(ShipmentStatus.DISETUJUI_ADMIN, resp.status());
            assertEquals(new BigDecimal("200"), resp.recognizedWeightKg());

            ArgumentCaptor<ShipmentApprovedByAdminEvent> captor =
                ArgumentCaptor.forClass(ShipmentApprovedByAdminEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertEquals("mandor-1", captor.getValue().mandorId());
        }

        @Test
        @DisplayName("should reject with reason")
        void rejectSuccess() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.DISETUJUI_MANDOR, "200");
            when(userGateway.isAdmin("admin-1")).thenReturn(true);
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            ShipmentResponse resp = service.reviewByAdmin(id,
                new AdminReviewRequest("admin-1", AdminReviewDecision.REJECT, null, "Kualitas buruk"));

            assertEquals(ShipmentStatus.DITOLAK_ADMIN, resp.status());
            assertEquals("Kualitas buruk", resp.rejectionReason());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should reject when admin reject reason is missing")
        void rejectMissingReason() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.DISETUJUI_MANDOR, "200");
            when(userGateway.isAdmin("admin-1")).thenReturn(true);
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            assertThrows(BusinessRuleViolationException.class,
                () -> service.reviewByAdmin(id,
                    new AdminReviewRequest("admin-1", AdminReviewDecision.REJECT, null, null)));
        }

        @Test
        @DisplayName("should partial reject and publish event")
        void partialRejectSuccess() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.DISETUJUI_MANDOR, "200");
            when(userGateway.isAdmin("admin-1")).thenReturn(true);
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            ShipmentResponse resp = service.reviewByAdmin(id,
                new AdminReviewRequest("admin-1", AdminReviewDecision.PARTIAL_REJECT,
                    new BigDecimal("125.5"), "Sebagian kurang"));

            assertEquals(ShipmentStatus.DITOLAK_PARSIAL_ADMIN, resp.status());
            assertEquals(new BigDecimal("125.5"), resp.recognizedWeightKg());

            ArgumentCaptor<ShipmentPartialRejectedByAdminEvent> captor =
                ArgumentCaptor.forClass(ShipmentPartialRejectedByAdminEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertEquals(new BigDecimal("125.5"), captor.getValue().recognizedWeightKg());
        }

        @Test
        @DisplayName("should reject partial when recognized weight >= total")
        void partialRejectOverweight() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.DISETUJUI_MANDOR, "200");
            when(userGateway.isAdmin("admin-1")).thenReturn(true);
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            assertThrows(BusinessRuleViolationException.class,
                () -> service.reviewByAdmin(id,
                    new AdminReviewRequest("admin-1", AdminReviewDecision.PARTIAL_REJECT,
                        new BigDecimal("220"), "Berat melebihi")));
        }

        @Test
        @DisplayName("should reject partial when recognized weight is null")
        void partialRejectMissingWeight() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.DISETUJUI_MANDOR, "200");
            when(userGateway.isAdmin("admin-1")).thenReturn(true);
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            assertThrows(BusinessRuleViolationException.class,
                () -> service.reviewByAdmin(id,
                    new AdminReviewRequest("admin-1", AdminReviewDecision.PARTIAL_REJECT,
                        null, "Reason")));
        }

        @Test
        @DisplayName("should reject partial when recognized weight is zero")
        void partialRejectZeroWeight() {
            UUID id = UUID.randomUUID();
            Shipment s = buildShipment(id, ShipmentStatus.DISETUJUI_MANDOR, "200");
            when(userGateway.isAdmin("admin-1")).thenReturn(true);
            when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

            assertThrows(BusinessRuleViolationException.class,
                () -> service.reviewByAdmin(id,
                    new AdminReviewRequest("admin-1", AdminReviewDecision.PARTIAL_REJECT,
                        BigDecimal.ZERO, "Reason")));
        }
    }

    // ─── Helpers ────────────────────────────────────────────────

    private Shipment buildShipment(UUID id, ShipmentStatus status, String weight) {
        Shipment s = new Shipment();
        s.setId(id);
        s.setDriverId("driver-1");
        s.setMandorId("mandor-1");
        s.setHarvestIds(new ArrayList<>(List.of("harvest-1")));
        s.setStatus(status);
        s.setTotalWeightKg(new BigDecimal(weight));
        return s;
    }
}
