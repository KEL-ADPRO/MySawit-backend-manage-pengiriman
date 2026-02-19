package com.mysawit.mysawit_pengiriman.repository;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PengirimanRepository extends JpaRepository<Pengiriman, UUID> {

}