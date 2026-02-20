package com.mysawit.mysawit_pengiriman.service;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import java.util.List;
import java.util.UUID;

public interface PengirimanService {
    Pengiriman create(Pengiriman pengiriman);
    List<Pengiriman> findAll();
    Pengiriman findById(UUID id);
    Pengiriman update(UUID id, Pengiriman pengirimanDetails);
    void delete(UUID id);
}