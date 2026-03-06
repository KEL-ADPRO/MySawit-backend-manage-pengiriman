package com.mysawit.mysawit_pengiriman.repository;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.model.StatusPengiriman;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PengirimanRepository extends JpaRepository<Pengiriman, UUID> {
    List<Pengiriman> findBySupirId(UUID supirId);
    List<Pengiriman> findByMandorId(UUID mandorId);
    List<Pengiriman> findByStatusAndTanggalDibuatBetween(StatusPengiriman status, LocalDateTime startDate, LocalDateTime endDate);
}