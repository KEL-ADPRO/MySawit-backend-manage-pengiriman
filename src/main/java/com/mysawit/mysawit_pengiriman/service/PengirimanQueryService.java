package com.mysawit.mysawit_pengiriman.service;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PengirimanQueryService {
    Pengiriman getDetail(UUID id);
    List<Pengiriman> getDaftarPengirimanSupir(UUID supirId, LocalDate startDate, LocalDate endDate);
    List<Pengiriman> getRiwayatPengirimanSupir(UUID supirId, LocalDate tanggal);
    List<Pengiriman> getPengirimanBerlangsungMandor(UUID mandorId, String supirNama);
    List<Pengiriman> getDaftarPengirimanSupirMandor(UUID mandorId, UUID supirId, LocalDate tanggal);
    List<Pengiriman> getDaftarPersetujuanAdmin(String mandorNama, LocalDate tanggal);
}
