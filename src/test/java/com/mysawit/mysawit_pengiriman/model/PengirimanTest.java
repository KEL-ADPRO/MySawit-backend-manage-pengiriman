package com.mysawit.mysawit_pengiriman.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PengirimanTest {

    private Pengiriman pengiriman;

    @BeforeEach
    void setUp() {
        pengiriman = new Pengiriman();
    }

    @Test
    void testCreatePengiriman() {
        UUID id = UUID.randomUUID();
        pengiriman.setId(id);
        pengiriman.setNama("Budi Supir");
        pengiriman.setTotalAngkutan(250.5);

        assertEquals(id, pengiriman.getId());
        assertEquals("Budi Supir", pengiriman.getNama());
        assertEquals(250.5, pengiriman.getTotalAngkutan());
    }
}