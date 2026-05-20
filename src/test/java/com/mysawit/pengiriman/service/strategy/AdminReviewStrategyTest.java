package com.mysawit.pengiriman.service.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mysawit.pengiriman.dto.AdminReviewRequest;
import com.mysawit.pengiriman.entity.Shipment;
import com.mysawit.pengiriman.enums.AdminReviewDecision;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.exception.BusinessRuleViolationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminReviewStrategyTest {

    @Nested
    @DisplayName("ApproveAdminStrategy")
    class ApproveTests {

        private final ApproveAdminStrategy strategy = new ApproveAdminStrategy();

        @Test
        @DisplayName("should set status to DISETUJUI_ADMIN and full weight")
        void approveSuccess() {
            Shipment s = buildShipment("200");
            AdminReviewRequest req = new AdminReviewRequest(
                "admin", AdminReviewDecision.APPROVE, null, null
            );

            strategy.execute(s, req);

            assertEquals(ShipmentStatus.DISETUJUI_ADMIN, s.getStatus());
            assertEquals(new BigDecimal("200"), s.getRecognizedWeightKg());
            assertNull(s.getRejectionReason());
        }
    }

    @Nested
    @DisplayName("RejectAdminStrategy")
    class RejectTests {

        private final RejectAdminStrategy strategy = new RejectAdminStrategy();

        @Test
        @DisplayName("should set status to DITOLAK_ADMIN with zero weight")
        void rejectSuccess() {
            Shipment s = buildShipment("200");
            AdminReviewRequest req = new AdminReviewRequest(
                "admin", AdminReviewDecision.REJECT, null, "Kualitas buruk"
            );

            strategy.execute(s, req);

            assertEquals(ShipmentStatus.DITOLAK_ADMIN, s.getStatus());
            assertEquals(BigDecimal.ZERO, s.getRecognizedWeightKg());
            assertEquals("Kualitas buruk", s.getRejectionReason());
        }

        @Test
        @DisplayName("should throw when reason is null")
        void rejectMissingReason() {
            Shipment s = buildShipment("200");
            AdminReviewRequest req = new AdminReviewRequest(
                "admin", AdminReviewDecision.REJECT, null, null
            );

            assertThrows(BusinessRuleViolationException.class,
                () -> strategy.execute(s, req));
        }

        @Test
        @DisplayName("should throw when reason is blank")
        void rejectBlankReason() {
            Shipment s = buildShipment("200");
            AdminReviewRequest req = new AdminReviewRequest(
                "admin", AdminReviewDecision.REJECT, null, "   "
            );

            assertThrows(BusinessRuleViolationException.class,
                () -> strategy.execute(s, req));
        }
    }

    @Nested
    @DisplayName("PartialRejectAdminStrategy")
    class PartialRejectTests {

        private final PartialRejectAdminStrategy strategy = new PartialRejectAdminStrategy();

        @Test
        @DisplayName("should set partial status with recognized weight")
        void partialRejectSuccess() {
            Shipment s = buildShipment("200");
            AdminReviewRequest req = new AdminReviewRequest(
                "admin", AdminReviewDecision.PARTIAL_REJECT,
                new BigDecimal("125"), "Sebagian rusak"
            );

            strategy.execute(s, req);

            assertEquals(ShipmentStatus.DITOLAK_PARSIAL_ADMIN, s.getStatus());
            assertEquals(new BigDecimal("125"), s.getRecognizedWeightKg());
            assertEquals("Sebagian rusak", s.getRejectionReason());
        }

        @Test
        @DisplayName("should throw when recognized weight is null")
        void throwNullWeight() {
            Shipment s = buildShipment("200");
            AdminReviewRequest req = new AdminReviewRequest(
                "admin", AdminReviewDecision.PARTIAL_REJECT, null, "Reason"
            );

            assertThrows(BusinessRuleViolationException.class,
                () -> strategy.execute(s, req));
        }

        @Test
        @DisplayName("should throw when recognized weight is zero")
        void throwZeroWeight() {
            Shipment s = buildShipment("200");
            AdminReviewRequest req = new AdminReviewRequest(
                "admin", AdminReviewDecision.PARTIAL_REJECT,
                BigDecimal.ZERO, "Reason"
            );

            assertThrows(BusinessRuleViolationException.class,
                () -> strategy.execute(s, req));
        }

        @Test
        @DisplayName("should throw when recognized weight >= total")
        void throwOverweight() {
            Shipment s = buildShipment("200");
            AdminReviewRequest req = new AdminReviewRequest(
                "admin", AdminReviewDecision.PARTIAL_REJECT,
                new BigDecimal("200"), "Reason"
            );

            assertThrows(BusinessRuleViolationException.class,
                () -> strategy.execute(s, req));
        }

        @Test
        @DisplayName("should throw when reason is missing")
        void throwMissingReason() {
            Shipment s = buildShipment("200");
            AdminReviewRequest req = new AdminReviewRequest(
                "admin", AdminReviewDecision.PARTIAL_REJECT,
                new BigDecimal("100"), null
            );

            assertThrows(BusinessRuleViolationException.class,
                () -> strategy.execute(s, req));
        }
    }

    @Nested
    @DisplayName("AdminReviewStrategyFactory")
    class FactoryTests {

        private final AdminReviewStrategyFactory factory = new AdminReviewStrategyFactory(
            new ApproveAdminStrategy(),
            new RejectAdminStrategy(),
            new PartialRejectAdminStrategy()
        );

        @Test
        @DisplayName("should resolve approve strategy")
        void resolveApprove() {
            AdminReviewStrategy s = factory.resolve(AdminReviewDecision.APPROVE);
            assertEquals(ApproveAdminStrategy.class, s.getClass());
        }

        @Test
        @DisplayName("should resolve reject strategy")
        void resolveReject() {
            AdminReviewStrategy s = factory.resolve(AdminReviewDecision.REJECT);
            assertEquals(RejectAdminStrategy.class, s.getClass());
        }

        @Test
        @DisplayName("should resolve partial reject strategy")
        void resolvePartialReject() {
            AdminReviewStrategy s = factory.resolve(AdminReviewDecision.PARTIAL_REJECT);
            assertEquals(PartialRejectAdminStrategy.class, s.getClass());
        }
    }

    private Shipment buildShipment(String weight) {
        Shipment s = new Shipment();
        s.setStatus(ShipmentStatus.DISETUJUI_MANDOR);
        s.setTotalWeightKg(new BigDecimal(weight));
        return s;
    }
}
