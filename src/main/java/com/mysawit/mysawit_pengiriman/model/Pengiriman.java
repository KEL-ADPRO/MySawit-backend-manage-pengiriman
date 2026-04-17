package com.mysawit.mysawit_pengiriman.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "pengiriman")
public class Pengiriman {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID supirId;

    @Column(nullable = false)
    private UUID mandorId;

    @Column(nullable = false)
    private String supirNama;

    @Column(nullable = false)
    private String mandorNama;

    @Column(nullable = false)
    private Double totalAngkutan;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pengiriman_hasil_panen", joinColumns = @JoinColumn(name = "pengiriman_id"))
    @Column(name = "hasil_panen_id", nullable = false)
    private List<UUID> hasilPanenIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPengiriman status;

    private String alasanPenolakan;

    private Double angkutanDiakui;

    @Column(nullable = false)
    private LocalDateTime tanggalDibuat = LocalDateTime.now();

    public boolean sedangBerlangsung() {
        return status == StatusPengiriman.MEMUAT
            || status == StatusPengiriman.MENGIRIM
            || status == StatusPengiriman.TIBA_DI_TUJUAN;
    }
}
