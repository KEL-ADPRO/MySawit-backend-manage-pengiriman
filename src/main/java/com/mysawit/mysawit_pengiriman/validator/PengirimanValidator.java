package com.mysawit.mysawit_pengiriman.validator;

import com.mysawit.mysawit_pengiriman.exception.PengirimanStateException;
import com.mysawit.mysawit_pengiriman.exception.PengirimanValidationException;
import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.model.StatusPengiriman;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

@Component
public class PengirimanValidator {

    private static final double MAX_KAPASITAS_ANGKUTAN = 400.0;

    public void validateAssignment(Pengiriman pengiriman) {
        if (pengiriman.getSupirId() == null || pengiriman.getMandorId() == null) {
            throw new PengirimanValidationException("Supir dan mandor wajib diisi");
        }
        if (isBlank(pengiriman.getSupirNama()) || isBlank(pengiriman.getMandorNama())) {
            throw new PengirimanValidationException("Nama supir dan nama mandor wajib diisi");
        }
        if (pengiriman.getTotalAngkutan() == null || pengiriman.getTotalAngkutan() <= 0) {
            throw new PengirimanValidationException("Total angkutan wajib lebih dari 0 Kg");
        }
        if (pengiriman.getTotalAngkutan() > MAX_KAPASITAS_ANGKUTAN) {
            throw new PengirimanValidationException("Kapasitas angkutan maksimal adalah 400 Kg");
        }
        if (pengiriman.getHasilPanenIds() == null || pengiriman.getHasilPanenIds().isEmpty()) {
            throw new PengirimanValidationException("Minimal harus ada satu hasil panen yang diangkut");
        }
        if (new HashSet<>(pengiriman.getHasilPanenIds()).size() != pengiriman.getHasilPanenIds().size()) {
            throw new PengirimanValidationException("Daftar hasil panen tidak boleh duplikat");
        }
    }

    public void validateSupirStatusTransition(StatusPengiriman currentStatus, StatusPengiriman nextStatus) {
        if (nextStatus == null) {
            throw new PengirimanValidationException("Status pengiriman wajib diisi");
        }

        List<StatusPengiriman> allowedStatuses = List.of(
            StatusPengiriman.MEMUAT,
            StatusPengiriman.MENGIRIM,
            StatusPengiriman.TIBA_DI_TUJUAN
        );
        if (!allowedStatuses.contains(nextStatus)) {
            throw new PengirimanValidationException("Supir tidak memiliki akses untuk menetapkan status ini");
        }

        boolean validTransition = (currentStatus == StatusPengiriman.MEMUAT && nextStatus == StatusPengiriman.MENGIRIM)
            || (currentStatus == StatusPengiriman.MENGIRIM && nextStatus == StatusPengiriman.TIBA_DI_TUJUAN);

        if (!validTransition) {
            throw new PengirimanStateException("Transisi status supir tidak valid");
        }
    }

    public void validateMandorVerification(Pengiriman pengiriman, boolean isApproved, String alasan) {
        if (pengiriman.getStatus() != StatusPengiriman.TIBA_DI_TUJUAN) {
            throw new PengirimanStateException("Hanya bisa diverifikasi jika status sudah Tiba Di Tujuan");
        }
        if (!isApproved && isBlank(alasan)) {
            throw new PengirimanValidationException("Penolakan wajib menyertakan alasan");
        }
    }

    public void validateAdminVerification(Pengiriman pengiriman, boolean isApproved, String alasan) {
        if (pengiriman.getStatus() != StatusPengiriman.DISETUJUI_MANDOR) {
            throw new PengirimanStateException("Hanya bisa diproses admin jika sudah disetujui mandor");
        }
        if (!isApproved && isBlank(alasan)) {
            throw new PengirimanValidationException("Penolakan wajib menyertakan alasan");
        }
    }

    public void validatePartialAdminRejection(Pengiriman pengiriman, String alasan, Double angkutanDiakui) {
        if (pengiriman.getStatus() != StatusPengiriman.DISETUJUI_MANDOR) {
            throw new PengirimanStateException("Hanya bisa diproses admin jika sudah disetujui mandor");
        }
        if (isBlank(alasan)) {
            throw new PengirimanValidationException("Penolakan parsial wajib menyertakan alasan");
        }
        if (angkutanDiakui == null || angkutanDiakui <= 0 || angkutanDiakui >= pengiriman.getTotalAngkutan()) {
            throw new PengirimanValidationException("Angkutan yang diakui tidak valid");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
