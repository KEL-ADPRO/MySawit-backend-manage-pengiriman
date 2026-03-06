package com.mysawit.mysawit_pengiriman.service;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.model.StatusPengiriman;

import java.util.List;
import java.util.UUID;

public interface PengirimanService {
    public Pengiriman assignPengiriman(Pengiriman pengiriman);
    public Pengiriman updateStatusBySupir(UUID id, StatusPengiriman statusBaru);
    public Pengiriman verifikasiOlehMandor(UUID id, boolean isApproved, String alasan);
    public Pengiriman verifikasiOlehAdmin(UUID id, boolean isApproved, String alasan);
    public Pengiriman tolakParsialOlehAdmin(UUID id, String alasan, Double angkutanDiakui);

}