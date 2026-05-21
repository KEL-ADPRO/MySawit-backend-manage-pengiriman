package com.mysawit.pengiriman.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShipmentStatusTest {

    @Test
    @DisplayName("MEMUAT can only transition to MENGIRIM")
    void memuatTransitions() {
        assertEquals(Set.of(ShipmentStatus.MENGIRIM),
            ShipmentStatus.MEMUAT.allowedTransitions());
        assertTrue(ShipmentStatus.MEMUAT.canTransitionTo(ShipmentStatus.MENGIRIM));
        assertFalse(ShipmentStatus.MEMUAT.canTransitionTo(ShipmentStatus.TIBA_DI_TUJUAN));
    }

    @Test
    @DisplayName("MENGIRIM can only transition to TIBA_DI_TUJUAN")
    void mengirimTransitions() {
        assertTrue(ShipmentStatus.MENGIRIM.canTransitionTo(ShipmentStatus.TIBA_DI_TUJUAN));
        assertFalse(ShipmentStatus.MENGIRIM.canTransitionTo(ShipmentStatus.MEMUAT));
    }

    @Test
    @DisplayName("TIBA_DI_TUJUAN can transition to DISETUJUI_MANDOR or DITOLAK_MANDOR")
    void tibaTransitions() {
        assertTrue(ShipmentStatus.TIBA_DI_TUJUAN.canTransitionTo(ShipmentStatus.DISETUJUI_MANDOR));
        assertTrue(ShipmentStatus.TIBA_DI_TUJUAN.canTransitionTo(ShipmentStatus.DITOLAK_MANDOR));
        assertFalse(ShipmentStatus.TIBA_DI_TUJUAN.canTransitionTo(ShipmentStatus.DISETUJUI_ADMIN));
    }

    @Test
    @DisplayName("DISETUJUI_MANDOR can transition to admin decisions")
    void disetujuiMandorTransitions() {
        assertTrue(ShipmentStatus.DISETUJUI_MANDOR.canTransitionTo(ShipmentStatus.DISETUJUI_ADMIN));
        assertTrue(ShipmentStatus.DISETUJUI_MANDOR.canTransitionTo(ShipmentStatus.DITOLAK_ADMIN));
        assertTrue(ShipmentStatus.DISETUJUI_MANDOR.canTransitionTo(ShipmentStatus.DITOLAK_PARSIAL_ADMIN));
    }

    @Test
    @DisplayName("Terminal statuses have no transitions")
    void terminalStatuses() {
        assertTrue(ShipmentStatus.DITOLAK_MANDOR.isTerminal());
        assertTrue(ShipmentStatus.DISETUJUI_ADMIN.isTerminal());
        assertTrue(ShipmentStatus.DITOLAK_ADMIN.isTerminal());
        assertTrue(ShipmentStatus.DITOLAK_PARSIAL_ADMIN.isTerminal());
    }

    @Test
    @DisplayName("isDriverMutable returns true only for driver-controlled states")
    void driverMutable() {
        assertTrue(ShipmentStatus.MEMUAT.isDriverMutable());
        assertTrue(ShipmentStatus.MENGIRIM.isDriverMutable());
        assertFalse(ShipmentStatus.TIBA_DI_TUJUAN.isDriverMutable());
        assertFalse(ShipmentStatus.DISETUJUI_MANDOR.isDriverMutable());
    }

    @Test
    @DisplayName("actor returns correct actor for each status")
    void actorCheck() {
        assertEquals("DRIVER", ShipmentStatus.MEMUAT.actor());
        assertEquals("DRIVER", ShipmentStatus.MENGIRIM.actor());
        assertEquals("MANDOR", ShipmentStatus.TIBA_DI_TUJUAN.actor());
        assertEquals("ADMIN", ShipmentStatus.DISETUJUI_MANDOR.actor());
        assertEquals("NONE", ShipmentStatus.DITOLAK_MANDOR.actor());
    }

    @Test
    @DisplayName("Non-terminal statuses are not terminal")
    void nonTerminal() {
        assertFalse(ShipmentStatus.MEMUAT.isTerminal());
        assertFalse(ShipmentStatus.MENGIRIM.isTerminal());
        assertFalse(ShipmentStatus.TIBA_DI_TUJUAN.isTerminal());
        assertFalse(ShipmentStatus.DISETUJUI_MANDOR.isTerminal());
    }
}
