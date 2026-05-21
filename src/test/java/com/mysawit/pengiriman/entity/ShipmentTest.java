package com.mysawit.pengiriman.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mysawit.pengiriman.enums.ShipmentStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShipmentTest {

    @Test
    @DisplayName("should set and get all fields")
    void testGettersAndSetters() {
        Shipment s = new Shipment();
        UUID id = UUID.randomUUID();
        s.setId(id);
        s.setDriverId("driver-1");
        s.setMandorId("mandor-1");
        s.setHarvestIds(new ArrayList<>(List.of("h1")));
        s.setStatus(ShipmentStatus.MEMUAT);
        s.setTotalWeightKg(new BigDecimal("200"));
        s.setRecognizedWeightKg(new BigDecimal("150"));
        s.setRejectionReason("reason");

        assertEquals(id, s.getId());
        assertEquals("driver-1", s.getDriverId());
        assertEquals("mandor-1", s.getMandorId());
        assertEquals(List.of("h1"), s.getHarvestIds());
        assertEquals(ShipmentStatus.MEMUAT, s.getStatus());
        assertEquals(new BigDecimal("200"), s.getTotalWeightKg());
        assertEquals(new BigDecimal("150"), s.getRecognizedWeightKg());
        assertEquals("reason", s.getRejectionReason());
    }

    @Test
    @DisplayName("onCreate should set createdAt and updatedAt")
    void testOnCreate() {
        Shipment s = new Shipment();
        s.onCreate();

        assertNotNull(s.getCreatedAt());
        assertNotNull(s.getUpdatedAt());
    }

    @Test
    @DisplayName("onUpdate should set updatedAt")
    void testOnUpdate() {
        Shipment s = new Shipment();
        s.onUpdate();

        assertNotNull(s.getUpdatedAt());
    }

    @Test
    @DisplayName("review timestamps can be set and retrieved")
    void testReviewTimestamps() {
        Shipment s = new Shipment();
        assertNull(s.getMandorReviewedAt());
        assertNull(s.getAdminReviewedAt());

        java.time.Instant now = java.time.Instant.now();
        s.setMandorReviewedAt(now);
        s.setAdminReviewedAt(now);

        assertEquals(now, s.getMandorReviewedAt());
        assertEquals(now, s.getAdminReviewedAt());
    }
}
