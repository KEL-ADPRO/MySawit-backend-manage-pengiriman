package com.mysawit.mysawit_pengiriman.repository;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.model.StatusPengiriman;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PengirimanRepositoryTest {

    @Autowired
    private PengirimanRepository pengirimanRepository;

    @Test
    void testFindCustomQueries_HarusBerhasil() {
        UUID supirId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();

        Pengiriman p1 = new Pengiriman();
        p1.setSupirId(supirId);
        p1.setMandorId(mandorId);
        p1.setTotalAngkutan(200.0);
        p1.setStatus(StatusPengiriman.MEMUAT);
        p1.setTanggalDibuat(LocalDateTime.now().minusDays(2));
        pengirimanRepository.save(p1);

        Pengiriman p2 = new Pengiriman();
        p2.setSupirId(supirId);
        p2.setMandorId(UUID.randomUUID());
        p2.setTotalAngkutan(300.0);
        p2.setStatus(StatusPengiriman.DISETUJUI_MANDOR);
        p2.setTanggalDibuat(LocalDateTime.now());
        pengirimanRepository.save(p2);

        List<Pengiriman> hasilSupir = pengirimanRepository.findBySupirId(supirId);
        assertEquals(2, hasilSupir.size());

        List<Pengiriman> hasilMandor = pengirimanRepository.findByMandorId(mandorId);
        assertEquals(1, hasilMandor.size());

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<Pengiriman> hasilFilter = pengirimanRepository.findByStatusAndTanggalDibuatBetween(
            StatusPengiriman.DISETUJUI_MANDOR, start, end
        );
        assertEquals(1, hasilFilter.size());
        assertEquals(300.0, hasilFilter.get(0).getTotalAngkutan());
    }
}