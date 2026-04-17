package com.mysawit.mysawit_pengiriman.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PengirimanTest {

    @Test
    void testPengirimanInitialization() {
        Pengiriman pengiriman = new Pengiriman();
        pengiriman.setSupirId(UUID.randomUUID());
        pengiriman.setMandorId(UUID.randomUUID());
        pengiriman.setSupirNama("Budi");
        pengiriman.setMandorNama("Rahmat");
        pengiriman.setTotalAngkutan(350.0);
        pengiriman.setHasilPanenIds(List.of(UUID.randomUUID()));
        pengiriman.setStatus(StatusPengiriman.MEMUAT);

        assertNotNull(pengiriman.getTanggalDibuat());

        assertNull(pengiriman.getAlasanPenolakan());
        assertNull(pengiriman.getAngkutanDiakui());
        assertTrue(pengiriman.sedangBerlangsung());
    }
}
