package com.mysawit.mysawit_pengiriman.service;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.model.StatusPengiriman;

import java.util.UUID;

public interface PengirimanCommandService {
    Pengiriman assignPengiriman(Pengiriman pengiriman);
    Pengiriman updateStatusBySupir(UUID id, StatusPengiriman statusBaru);
    Pengiriman verifikasiOlehMandor(UUID id, boolean isApproved, String alasan);
    Pengiriman verifikasiOlehAdmin(UUID id, boolean isApproved, String alasan);
    Pengiriman tolakParsialOlehAdmin(UUID id, String alasan, Double angkutanDiakui);
}
