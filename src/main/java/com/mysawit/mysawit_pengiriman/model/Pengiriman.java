package com.mysawit.mysawit_pengiriman.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
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
    private Double totalAngkutan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPengiriman status;

    private String alasanPenolakan;

    private Double angkutanDiakui;

    @Column(nullable = false)
    private LocalDateTime tanggalDibuat = LocalDateTime.now();
}