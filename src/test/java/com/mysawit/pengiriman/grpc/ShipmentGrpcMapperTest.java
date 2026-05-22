package com.mysawit.pengiriman.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.proto.RecognizedWeightResponse;
import com.mysawit.pengiriman.proto.ShipmentMessage;
import com.mysawit.pengiriman.proto.ShipmentStatusGrpc;
import com.mysawit.pengiriman.proto.ShipmentSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShipmentGrpcMapperTest {

    private final ShipmentGrpcMapper mapper = new ShipmentGrpcMapper();

    // ─── toGrpc (ShipmentMessage) ────────────────────────────────────

    @Test
    @DisplayName("toGrpc should map all fields including recognized weight")
    void toGrpc() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        ShipmentResponse resp = new ShipmentResponse(
            id, "d1", "m1", List.of("h1"),
            new BigDecimal("200"), new BigDecimal("150"),
            ShipmentStatus.DITOLAK_PARSIAL_ADMIN, "Reason",
            now, now, now, now
        );

        ShipmentMessage msg = mapper.toGrpc(resp);

        assertEquals(id.toString(), msg.getId());
        assertEquals("d1", msg.getDriverId());
        assertEquals("m1", msg.getMandorId());
        assertEquals("200", msg.getTotalWeightKg());
        assertEquals("150", msg.getRecognizedWeightKg());
        assertEquals(ShipmentStatusGrpc.DITOLAK_PARSIAL_ADMIN, msg.getStatus());
        assertEquals("Reason", msg.getRejectionReason());
    }

    @Test
    @DisplayName("toGrpc should handle null optional fields gracefully")
    void toGrpcNulls() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        ShipmentResponse resp = new ShipmentResponse(
            id, "d1", "m1", List.of(),
            new BigDecimal("100"), null,
            ShipmentStatus.MEMUAT, null,
            now, now, null, null
        );

        ShipmentMessage msg = mapper.toGrpc(resp);

        assertEquals("", msg.getRecognizedWeightKg());
        assertEquals("", msg.getRejectionReason());
        assertEquals("", msg.getMandorReviewedAt());
        assertEquals("", msg.getAdminReviewedAt());
    }

    // ─── toSummary (ShipmentSummary — payment-compatible) ────────────

    @Test
    @DisplayName("toSummary should use payment-compatible field names")
    void toSummary() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        ShipmentResponse resp = new ShipmentResponse(
            id, "driver-1", "mandor-1", List.of("h1"),
            new BigDecimal("200"), new BigDecimal("150"),
            ShipmentStatus.DISETUJUI_ADMIN, null,
            now, now, now, now
        );

        ShipmentSummary summary = mapper.toSummary(resp);

        assertEquals(id.toString(), summary.getId());
        assertEquals("DISETUJUI_ADMIN", summary.getStatus());
        assertEquals("driver-1", summary.getSupirUserId());
        assertEquals("mandor-1", summary.getMandorUserId());
        assertEquals("200", summary.getDeliveredKg());
        assertEquals("150", summary.getRecognizedKg());
    }

    @Test
    @DisplayName("toSummary should return empty string for recognized_kg if null")
    void toSummaryNullRecognized() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        ShipmentResponse resp = new ShipmentResponse(
            id, "d1", "m1", List.of(),
            new BigDecimal("100"), null,
            ShipmentStatus.DISETUJUI_MANDOR, null,
            now, now, now, null
        );

        ShipmentSummary summary = mapper.toSummary(resp);

        assertEquals("", summary.getRecognizedKg());
        assertEquals("", summary.getAdminReviewedAt());
    }

    // ─── toRecognizedWeight ───────────────────────────────────────────

    @Test
    @DisplayName("toRecognizedWeight should return recognized_kg and shipment_id")
    void toRecognizedWeight() {
        UUID id = UUID.randomUUID();
        ShipmentResponse resp = new ShipmentResponse(
            id, "d1", "m1", List.of(),
            new BigDecimal("200"), new BigDecimal("125"),
            ShipmentStatus.DITOLAK_PARSIAL_ADMIN, null,
            Instant.now(), Instant.now(), Instant.now(), Instant.now()
        );

        RecognizedWeightResponse result = mapper.toRecognizedWeight(resp);

        assertEquals(id.toString(), result.getShipmentId());
        assertEquals("125", result.getRecognizedKg());
    }

    @Test
    @DisplayName("toRecognizedWeight should return empty string if recognized_kg is null")
    void toRecognizedWeightNull() {
        UUID id = UUID.randomUUID();
        ShipmentResponse resp = new ShipmentResponse(
            id, "d1", "m1", List.of(),
            new BigDecimal("200"), null,
            ShipmentStatus.DISETUJUI_MANDOR, null,
            Instant.now(), Instant.now(), Instant.now(), null
        );

        RecognizedWeightResponse result = mapper.toRecognizedWeight(resp);

        assertEquals(id.toString(), result.getShipmentId());
        assertEquals("", result.getRecognizedKg());
    }

    // ─── Status converters ────────────────────────────────────────────

    @Test
    @DisplayName("toGrpcStatus should convert domain status to gRPC enum")
    void toGrpcStatus() {
        assertEquals(ShipmentStatusGrpc.MEMUAT,
            mapper.toGrpcStatus(ShipmentStatus.MEMUAT));
        assertEquals(ShipmentStatusGrpc.DISETUJUI_ADMIN,
            mapper.toGrpcStatus(ShipmentStatus.DISETUJUI_ADMIN));
    }

    @Test
    @DisplayName("toDomainStatus should convert gRPC status to domain enum")
    void toDomainStatus() {
        assertEquals(ShipmentStatus.MENGIRIM,
            mapper.toDomainStatus(ShipmentStatusGrpc.MENGIRIM));
    }

    @Test
    @DisplayName("toDomainStatus should throw for UNSPECIFIED")
    void toDomainStatusUnspecified() {
        assertThrows(IllegalArgumentException.class,
            () -> mapper.toDomainStatus(ShipmentStatusGrpc.SHIPMENT_STATUS_UNSPECIFIED));
    }

    @Test
    @DisplayName("parseWeight should convert string to BigDecimal")
    void parseWeight() {
        assertEquals(new BigDecimal("123.45"), mapper.parseWeight("123.45"));
    }
}
