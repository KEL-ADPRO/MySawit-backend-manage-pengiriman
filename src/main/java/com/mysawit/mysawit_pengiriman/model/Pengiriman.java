package com.mysawit.mysawit_pengiriman.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
    private String nama;

    @Column(nullable = false)
    private Double totalAngkutan;
}