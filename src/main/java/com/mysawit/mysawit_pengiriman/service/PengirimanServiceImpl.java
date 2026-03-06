package com.mysawit.mysawit_pengiriman.service;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.model.StatusPengiriman;
import com.mysawit.mysawit_pengiriman.repository.PengirimanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PengirimanServiceImpl implements PengirimanService {

    @Autowired
    private PengirimanRepository pengirimanRepository;

    private Pengiriman getPengirimanOrThrow(UUID id) {
        return pengirimanRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Data Pengiriman tidak ditemukan!"));
    }

    @Override
    public Pengiriman assignPengiriman(Pengiriman pengiriman) {
        if (pengiriman.getTotalAngkutan() > 400.0) {
            throw new IllegalArgumentException("Kapasitas angkutan maksimal adalah 400 Kg");
        }
        pengiriman.setStatus(StatusPengiriman.MEMUAT);
        return pengirimanRepository.save(pengiriman);
    }

    @Override
    public Pengiriman updateStatusBySupir(UUID id, StatusPengiriman statusBaru) {
        Pengiriman pengiriman = getPengirimanOrThrow(id);

        // Supir hanya boleh mengubah ke status perjalanan logistik
        if (statusBaru != StatusPengiriman.MEMUAT &&
            statusBaru != StatusPengiriman.MENGIRIM &&
            statusBaru != StatusPengiriman.TIBA_DI_TUJUAN) {
            throw new IllegalArgumentException("Supir tidak memiliki akses untuk menetapkan status ini");
        }

        pengiriman.setStatus(statusBaru);
        return pengirimanRepository.save(pengiriman);
    }

    @Override
    public Pengiriman verifikasiOlehMandor(UUID id, boolean isApproved, String alasan) {
        Pengiriman pengiriman = getPengirimanOrThrow(id);

        if (pengiriman.getStatus() != StatusPengiriman.TIBA_DI_TUJUAN) {
            throw new IllegalStateException("Hanya bisa diverifikasi jika status sudah Tiba Di Tujuan");
        }

        if (!isApproved) {
            if (alasan == null || alasan.trim().isEmpty()) {
                throw new IllegalArgumentException("Penolakan wajib menyertakan alasan");
            }
            pengiriman.setStatus(StatusPengiriman.DITOLAK_MANDOR);
            pengiriman.setAlasanPenolakan(alasan);
        } else {
            pengiriman.setStatus(StatusPengiriman.DISETUJUI_MANDOR);
        }

        return pengirimanRepository.save(pengiriman);
    }

    @Override
    public Pengiriman verifikasiOlehAdmin(UUID id, boolean isApproved, String alasan) {
        Pengiriman pengiriman = getPengirimanOrThrow(id);

        if (pengiriman.getStatus() != StatusPengiriman.DISETUJUI_MANDOR) {
            throw new IllegalStateException("Hanya bisa diproses admin jika sudah disetujui mandor");
        }

        if (!isApproved) {
            if (alasan == null || alasan.trim().isEmpty()) {
                throw new IllegalArgumentException("Penolakan wajib menyertakan alasan");
            }
            pengiriman.setStatus(StatusPengiriman.DITOLAK_ADMIN);
            pengiriman.setAlasanPenolakan(alasan);
        } else {
            pengiriman.setStatus(StatusPengiriman.DISETUJUI_ADMIN);
            pengiriman.setAngkutanDiakui(pengiriman.getTotalAngkutan());
        }

        return pengirimanRepository.save(pengiriman);
    }

    @Override
    public Pengiriman tolakParsialOlehAdmin(UUID id, String alasan, Double angkutanDiakui) {
        Pengiriman pengiriman = getPengirimanOrThrow(id);

        if (pengiriman.getStatus() != StatusPengiriman.DISETUJUI_MANDOR) {
            throw new IllegalStateException("Hanya bisa diproses admin jika sudah disetujui mandor");
        }
        if (alasan == null || alasan.trim().isEmpty()) {
            throw new IllegalArgumentException("Penolakan parsial wajib menyertakan alasan");
        }
        if (angkutanDiakui == null || angkutanDiakui < 0 || angkutanDiakui >= pengiriman.getTotalAngkutan()) {
            throw new IllegalArgumentException("Angkutan yang diakui tidak valid");
        }

        pengiriman.setStatus(StatusPengiriman.DITOLAK_PARSIAL_ADMIN);
        pengiriman.setAlasanPenolakan(alasan);
        pengiriman.setAngkutanDiakui(angkutanDiakui);

        return pengirimanRepository.save(pengiriman);
    }
}