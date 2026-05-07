package com.mysawit.pengiriman.enums;

public enum ShipmentStatus {
    MEMUAT,
    MENGIRIM,
    TIBA_DI_TUJUAN,
    DISETUJUI_MANDOR,
    DITOLAK_MANDOR,
    DISETUJUI_ADMIN,
    DITOLAK_ADMIN,
    DITOLAK_PARSIAL_ADMIN;

    public boolean isDriverMutable() {
        return this == MEMUAT || this == MENGIRIM || this == TIBA_DI_TUJUAN;
    }
}
