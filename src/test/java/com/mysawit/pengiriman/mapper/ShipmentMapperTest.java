package com.mysawit.pengiriman.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.entity.Shipment;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShipmentMapperTest {

    private final ShipmentMapper mapper = new ShipmentMapper();

    @Test
    @DisplayName("should map entity to response correctly")
    void mapToResponse() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Shipment s = new Shipment();
        s.setId(id);
        s.setDriverId("driver-1");
        s.setMandorId("mandor-1");
        s.setHarvestIds(new ArrayList<>(List.of("h1", "h2")));
        s.setTotalWeightKg(new BigDecimal("200"));
        s.setRecognizedWeightKg(new BigDecimal("150"));
        s.setStatus(ShipmentStatus.DITOLAK_PARSIAL_ADMIN);
        s.setRejectionReason("Reason");
        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        s.setMandorReviewedAt(now);
        s.setAdminReviewedAt(now);

        ShipmentResponse r = mapper.toResponse(s);

        assertEquals(id, r.id());
        assertEquals("driver-1", r.driverId());
        assertEquals("mandor-1", r.mandorId());
        assertEquals(List.of("h1", "h2"), r.harvestIds());
        assertEquals(new BigDecimal("200"), r.totalWeightKg());
        assertEquals(new BigDecimal("150"), r.recognizedWeightKg());
        assertEquals(ShipmentStatus.DITOLAK_PARSIAL_ADMIN, r.status());
        assertEquals("Reason", r.rejectionReason());
        assertEquals(now, r.createdAt());
        assertEquals(now, r.mandorReviewedAt());
        assertEquals(now, r.adminReviewedAt());
    }

    @Test
    @DisplayName("should handle null optional fields")
    void mapNullOptionalFields() {
        Shipment s = new Shipment();
        s.setId(UUID.randomUUID());
        s.setDriverId("d1");
        s.setMandorId("m1");
        s.setHarvestIds(new ArrayList<>());
        s.setTotalWeightKg(BigDecimal.ZERO);
        s.setStatus(ShipmentStatus.MEMUAT);
        s.setCreatedAt(Instant.now());
        s.setUpdatedAt(Instant.now());

        ShipmentResponse r = mapper.toResponse(s);

        assertNull(r.recognizedWeightKg());
        assertNull(r.rejectionReason());
        assertNull(r.mandorReviewedAt());
        assertNull(r.adminReviewedAt());
    }
}
