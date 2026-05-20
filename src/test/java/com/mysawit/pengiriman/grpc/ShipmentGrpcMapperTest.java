package com.mysawit.pengiriman.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.proto.ShipmentMessage;
import com.mysawit.pengiriman.proto.ShipmentStatusGrpc;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShipmentGrpcMapperTest {

    private final ShipmentGrpcMapper mapper = new ShipmentGrpcMapper();

    @Test
    @DisplayName("should map ShipmentResponse to gRPC message")
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
    @DisplayName("should handle null optional fields in gRPC mapping")
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

    @Test
    @DisplayName("should convert domain status to gRPC status")
    void toGrpcStatus() {
        assertEquals(ShipmentStatusGrpc.MEMUAT,
            mapper.toGrpcStatus(ShipmentStatus.MEMUAT));
        assertEquals(ShipmentStatusGrpc.DISETUJUI_ADMIN,
            mapper.toGrpcStatus(ShipmentStatus.DISETUJUI_ADMIN));
    }

    @Test
    @DisplayName("should convert gRPC status to domain status")
    void toDomainStatus() {
        assertEquals(ShipmentStatus.MENGIRIM,
            mapper.toDomainStatus(ShipmentStatusGrpc.MENGIRIM));
    }

    @Test
    @DisplayName("should throw for UNSPECIFIED gRPC status")
    void toDomainStatusUnspecified() {
        assertThrows(IllegalArgumentException.class,
            () -> mapper.toDomainStatus(ShipmentStatusGrpc.SHIPMENT_STATUS_UNSPECIFIED));
    }

    @Test
    @DisplayName("should parse weight string to BigDecimal")
    void parseWeight() {
        assertEquals(new BigDecimal("123.45"), mapper.parseWeight("123.45"));
    }
}
