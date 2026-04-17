package com.mysawit.mysawit_pengiriman.repository;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.model.StatusPengiriman;
import com.mysawit.mysawit_pengiriman.service.criteria.PengirimanSearchCriteria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PengirimanRepositoryTest {

    @Autowired
    private PengirimanRepository pengirimanRepository;

    @Test
    void testFindBySpecification_HarusBerhasil() {
        UUID supirId = UUID.randomUUID();
        UUID mandorId = UUID.randomUUID();

        Pengiriman p1 = new Pengiriman();
        p1.setSupirId(supirId);
        p1.setMandorId(mandorId);
        p1.setSupirNama("Budi");
        p1.setMandorNama("Rahmat");
        p1.setTotalAngkutan(200.0);
        p1.setHasilPanenIds(List.of(UUID.randomUUID()));
        p1.setStatus(StatusPengiriman.MEMUAT);
        p1.setTanggalDibuat(LocalDateTime.now().minusDays(2));
        pengirimanRepository.save(p1);

        Pengiriman p2 = new Pengiriman();
        p2.setSupirId(supirId);
        p2.setMandorId(UUID.randomUUID());
        p2.setSupirNama("Budi");
        p2.setMandorNama("Rahmat");
        p2.setTotalAngkutan(300.0);
        p2.setHasilPanenIds(List.of(UUID.randomUUID(), UUID.randomUUID()));
        p2.setStatus(StatusPengiriman.DISETUJUI_MANDOR);
        p2.setTanggalDibuat(LocalDateTime.now());
        pengirimanRepository.save(p2);

        PengirimanSearchCriteria supirCriteria = PengirimanSearchCriteria.builder()
            .supirId(supirId)
            .build();
        List<Pengiriman> hasilSupir = pengirimanRepository.findAll(
            PengirimanSpecifications.withCriteria(supirCriteria),
            Sort.by(Sort.Direction.DESC, "tanggalDibuat")
        );
        assertEquals(2, hasilSupir.size());

        PengirimanSearchCriteria mandorCriteria = PengirimanSearchCriteria.builder()
            .mandorId(mandorId)
            .build();
        List<Pengiriman> hasilMandor = pengirimanRepository.findAll(
            PengirimanSpecifications.withCriteria(mandorCriteria),
            Sort.by(Sort.Direction.DESC, "tanggalDibuat")
        );
        assertEquals(1, hasilMandor.size());

        PengirimanSearchCriteria approvalCriteria = PengirimanSearchCriteria.builder()
            .statuses(List.of(StatusPengiriman.DISETUJUI_MANDOR))
            .mandorNama("rah")
            .startDateTime(LocalDateTime.now().minusDays(1))
            .endDateTime(LocalDateTime.now().plusDays(1))
            .build();
        List<Pengiriman> hasilFilter = pengirimanRepository.findAll(
            PengirimanSpecifications.withCriteria(approvalCriteria),
            Sort.by(Sort.Direction.DESC, "tanggalDibuat")
        );
        assertEquals(1, hasilFilter.size());
        assertEquals(300.0, hasilFilter.get(0).getTotalAngkutan());
    }
}
