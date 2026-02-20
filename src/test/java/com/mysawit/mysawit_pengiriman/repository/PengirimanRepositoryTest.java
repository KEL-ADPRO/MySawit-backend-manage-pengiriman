package com.mysawit.mysawit_pengiriman.repository;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PengirimanRepositoryTest {

    @Autowired
    private PengirimanRepository pengirimanRepository;

    @Test
    void testSaveAndFind() {
        Pengiriman pengiriman = new Pengiriman();
        pengiriman.setNama("Joko");
        pengiriman.setTotalAngkutan(100.0);

        // Simpan ke database bohongan (H2)
        Pengiriman saved = pengirimanRepository.save(pengiriman);

        // Pastikan ID berhasil di-generate (UUID)
        assertNotNull(saved.getId());

        // Cari datanya kembali
        Pengiriman found = pengirimanRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("Joko", found.getNama());
    }
}