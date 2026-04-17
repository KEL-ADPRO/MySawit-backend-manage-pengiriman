package com.mysawit.mysawit_pengiriman.service;

import com.mysawit.mysawit_pengiriman.exception.PengirimanNotFoundException;
import com.mysawit.mysawit_pengiriman.exception.PengirimanStateException;
import com.mysawit.mysawit_pengiriman.exception.PengirimanValidationException;
import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.model.StatusPengiriman;
import com.mysawit.mysawit_pengiriman.repository.PengirimanRepository;
import com.mysawit.mysawit_pengiriman.repository.PengirimanSpecifications;
import com.mysawit.mysawit_pengiriman.service.criteria.PengirimanSearchCriteria;
import com.mysawit.mysawit_pengiriman.validator.PengirimanValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PengirimanServiceImpl implements PengirimanService {

    @Autowired
    private PengirimanRepository pengirimanRepository;

    @Autowired
    private PengirimanValidator pengirimanValidator;

    private Pengiriman getPengirimanOrThrow(UUID id) {
        return pengirimanRepository.findById(id)
            .orElseThrow(() -> new PengirimanNotFoundException("Data pengiriman tidak ditemukan"));
    }

    private List<Pengiriman> findByCriteria(PengirimanSearchCriteria criteria) {
        return pengirimanRepository.findAll(
            PengirimanSpecifications.withCriteria(criteria),
            Sort.by(Sort.Direction.DESC, "tanggalDibuat")
        );
    }

    private LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay().minusNanos(1);
    }

    @Override
    public Pengiriman assignPengiriman(Pengiriman pengiriman) {
        pengirimanValidator.validateAssignment(pengiriman);
        pengiriman.setStatus(StatusPengiriman.MEMUAT);
        pengiriman.setAlasanPenolakan(null);
        pengiriman.setAngkutanDiakui(null);
        return pengirimanRepository.save(pengiriman);
    }

    @Override
    public Pengiriman updateStatusBySupir(UUID id, StatusPengiriman statusBaru) {
        Pengiriman pengiriman = getPengirimanOrThrow(id);
        pengirimanValidator.validateSupirStatusTransition(pengiriman.getStatus(), statusBaru);
        pengiriman.setStatus(statusBaru);
        return pengirimanRepository.save(pengiriman);
    }

    @Override
    public Pengiriman verifikasiOlehMandor(UUID id, boolean isApproved, String alasan) {
        Pengiriman pengiriman = getPengirimanOrThrow(id);
        pengirimanValidator.validateMandorVerification(pengiriman, isApproved, alasan);

        if (!isApproved) {
            pengiriman.setStatus(StatusPengiriman.DITOLAK_MANDOR);
            pengiriman.setAlasanPenolakan(alasan);
            pengiriman.setAngkutanDiakui(null);
        } else {
            pengiriman.setStatus(StatusPengiriman.DISETUJUI_MANDOR);
            pengiriman.setAlasanPenolakan(null);
        }

        return pengirimanRepository.save(pengiriman);
    }

    @Override
    public Pengiriman verifikasiOlehAdmin(UUID id, boolean isApproved, String alasan) {
        Pengiriman pengiriman = getPengirimanOrThrow(id);
        pengirimanValidator.validateAdminVerification(pengiriman, isApproved, alasan);

        if (!isApproved) {
            pengiriman.setStatus(StatusPengiriman.DITOLAK_ADMIN);
            pengiriman.setAlasanPenolakan(alasan);
            pengiriman.setAngkutanDiakui(null);
        } else {
            pengiriman.setStatus(StatusPengiriman.DISETUJUI_ADMIN);
            pengiriman.setAngkutanDiakui(pengiriman.getTotalAngkutan());
            pengiriman.setAlasanPenolakan(null);
        }

        return pengirimanRepository.save(pengiriman);
    }

    @Override
    public Pengiriman tolakParsialOlehAdmin(UUID id, String alasan, Double angkutanDiakui) {
        Pengiriman pengiriman = getPengirimanOrThrow(id);
        pengirimanValidator.validatePartialAdminRejection(pengiriman, alasan, angkutanDiakui);
        pengiriman.setStatus(StatusPengiriman.DITOLAK_PARSIAL_ADMIN);
        pengiriman.setAlasanPenolakan(alasan);
        pengiriman.setAngkutanDiakui(angkutanDiakui);

        return pengirimanRepository.save(pengiriman);
    }

    @Override
    public Pengiriman getDetail(UUID id) {
        return getPengirimanOrThrow(id);
    }

    @Override
    public List<Pengiriman> getDaftarPengirimanSupir(UUID supirId, LocalDate startDate, LocalDate endDate) {
        if (supirId == null) {
            throw new PengirimanValidationException("Supir ID wajib diisi");
        }

        PengirimanSearchCriteria.PengirimanSearchCriteriaBuilder builder = PengirimanSearchCriteria.builder()
            .supirId(supirId);

        if (startDate != null) {
            builder.startDateTime(startOfDay(startDate));
        }
        if (endDate != null) {
            builder.endDateTime(endOfDay(endDate));
        }

        return findByCriteria(builder.build());
    }

    @Override
    public List<Pengiriman> getRiwayatPengirimanSupir(UUID supirId, LocalDate tanggal) {
        if (supirId == null) {
            throw new PengirimanValidationException("Supir ID wajib diisi");
        }

        PengirimanSearchCriteria.PengirimanSearchCriteriaBuilder builder = PengirimanSearchCriteria.builder()
            .supirId(supirId)
            .statuses(List.of(
                StatusPengiriman.DISETUJUI_MANDOR,
                StatusPengiriman.DITOLAK_MANDOR,
                StatusPengiriman.DISETUJUI_ADMIN,
                StatusPengiriman.DITOLAK_ADMIN,
                StatusPengiriman.DITOLAK_PARSIAL_ADMIN
            ));

        if (tanggal != null) {
            builder.startDateTime(startOfDay(tanggal));
            builder.endDateTime(endOfDay(tanggal));
        }

        return findByCriteria(builder.build());
    }

    @Override
    public List<Pengiriman> getPengirimanBerlangsungMandor(UUID mandorId, String supirNama) {
        if (mandorId == null) {
            throw new PengirimanValidationException("Mandor ID wajib diisi");
        }

        return findByCriteria(PengirimanSearchCriteria.builder()
            .mandorId(mandorId)
            .supirNama(supirNama)
            .statuses(List.of(
                StatusPengiriman.MEMUAT,
                StatusPengiriman.MENGIRIM,
                StatusPengiriman.TIBA_DI_TUJUAN
            ))
            .build());
    }

    @Override
    public List<Pengiriman> getDaftarPengirimanSupirMandor(UUID mandorId, UUID supirId, LocalDate tanggal) {
        if (mandorId == null || supirId == null) {
            throw new PengirimanValidationException("Mandor ID dan Supir ID wajib diisi");
        }

        PengirimanSearchCriteria.PengirimanSearchCriteriaBuilder builder = PengirimanSearchCriteria.builder()
            .mandorId(mandorId)
            .supirId(supirId);

        if (tanggal != null) {
            builder.startDateTime(startOfDay(tanggal));
            builder.endDateTime(endOfDay(tanggal));
        }

        return findByCriteria(builder.build());
    }

    @Override
    public List<Pengiriman> getDaftarPersetujuanAdmin(String mandorNama, LocalDate tanggal) {
        PengirimanSearchCriteria.PengirimanSearchCriteriaBuilder builder = PengirimanSearchCriteria.builder()
            .statuses(List.of(StatusPengiriman.DISETUJUI_MANDOR));

        if (mandorNama != null && !mandorNama.isBlank()) {
            builder.mandorNama(mandorNama);
        }
        if (tanggal != null) {
            builder.startDateTime(startOfDay(tanggal));
            builder.endDateTime(endOfDay(tanggal));
        }

        return findByCriteria(builder.build());
    }
}
