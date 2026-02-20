package com.mysawit.mysawit_pengiriman.service;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.repository.PengirimanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class PengirimanServiceImpl implements PengirimanService {

    @Autowired
    private PengirimanRepository pengirimanRepository;

    @Override
    public Pengiriman create(Pengiriman pengiriman) {
        return pengirimanRepository.save(pengiriman);
    }

    @Override
    public List<Pengiriman> findAll() {
        return pengirimanRepository.findAll();
    }

    @Override
    public  Pengiriman findById(UUID id) {
        return pengirimanRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Data Pengiriman dengan ID " + id + " tidak ditemukan!"));
    }

    @Override
    public Pengiriman update(UUID id, Pengiriman pengirimanDetails) {
        Pengiriman pengirimanLama = findById(id);

        pengirimanLama.setNama(pengirimanDetails.getNama());
        pengirimanLama.setTotalAngkutan(pengirimanDetails.getTotalAngkutan());

        return pengirimanRepository.save(pengirimanLama);
    }

    @Override
    public void delete(UUID id) {
        Pengiriman pengiriman = findById(id);
        pengirimanRepository.delete(pengiriman);
    }
}