package com.mysawit.mysawit_pengiriman.service;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.model.StatusPengiriman;
import com.mysawit.mysawit_pengiriman.repository.PengirimanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PengirimanServiceImplTest {

    @Mock
    private PengirimanRepository pengirimanRepository;

    @InjectMocks
    private PengirimanServiceImpl pengirimanService;

    private Pengiriman pengiriman;
    private UUID dummyId;

    @BeforeEach
    void setUp() {
        dummyId = UUID.randomUUID();
        pengiriman = new Pengiriman();
        pengiriman.setId(dummyId);
        pengiriman.setSupirId(UUID.randomUUID());
        pengiriman.setMandorId(UUID.randomUUID());
    }

    @Test
    void testAssignPengiriman_LebihDari400Kg_HarusGagal() {
        pengiriman.setTotalAngkutan(450.0);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> pengirimanService.assignPengiriman(pengiriman));
        assertEquals("Kapasitas angkutan maksimal adalah 400 Kg", exception.getMessage());
    }

    @Test
    void testAssignPengiriman_Valid_StatusDefaultMemuat() {
        pengiriman.setTotalAngkutan(400.0);
        when(pengirimanRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        Pengiriman result = pengirimanService.assignPengiriman(pengiriman);
        assertEquals(StatusPengiriman.MEMUAT, result.getStatus());
    }

    @Test
    void testSupirUpdateStatus_Valid() {
        pengiriman.setStatus(StatusPengiriman.MEMUAT);
        when(pengirimanRepository.findById(dummyId)).thenReturn(Optional.of(pengiriman));
        when(pengirimanRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        Pengiriman result = pengirimanService.updateStatusBySupir(dummyId, StatusPengiriman.MENGIRIM);
        assertEquals(StatusPengiriman.MENGIRIM, result.getStatus());
    }

    @Test
    void testSupirUpdateStatus_ManipulasiStatusAdmin_HarusGagal() {
        pengiriman.setStatus(StatusPengiriman.TIBA_DI_TUJUAN);
        when(pengirimanRepository.findById(dummyId)).thenReturn(Optional.of(pengiriman));

        assertThrows(IllegalArgumentException.class, () ->
            pengirimanService.updateStatusBySupir(dummyId, StatusPengiriman.DISETUJUI_MANDOR)
        );
    }

    @Test
    void testMandorReject_TanpaAlasan_HarusGagal() {
        pengiriman.setStatus(StatusPengiriman.TIBA_DI_TUJUAN);
        when(pengirimanRepository.findById(dummyId)).thenReturn(Optional.of(pengiriman));

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            pengirimanService.verifikasiOlehMandor(dummyId, false, null)
        );
        assertEquals("Penolakan wajib menyertakan alasan", exception.getMessage());
    }

    @Test
    void testMandorApprove_Berhasil() {
        pengiriman.setStatus(StatusPengiriman.TIBA_DI_TUJUAN);
        when(pengirimanRepository.findById(dummyId)).thenReturn(Optional.of(pengiriman));
        when(pengirimanRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        Pengiriman result = pengirimanService.verifikasiOlehMandor(dummyId, true, null);
        assertEquals(StatusPengiriman.DISETUJUI_MANDOR, result.getStatus());
    }

    @Test
    void testAdminPartialReject_TanpaAngkaValid_HarusGagal() {
        pengiriman.setStatus(StatusPengiriman.DISETUJUI_MANDOR);
        pengiriman.setTotalAngkutan(300.0);
        when(pengirimanRepository.findById(dummyId)).thenReturn(Optional.of(pengiriman));

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            pengirimanService.tolakParsialOlehAdmin(dummyId, "Buah busuk sebagian", 350.0)
        );
        assertEquals("Angkutan yang diakui tidak valid", exception.getMessage());
    }

    @Test
    void testAdminPartialReject_Berhasil() {
        pengiriman.setStatus(StatusPengiriman.DISETUJUI_MANDOR);
        pengiriman.setTotalAngkutan(300.0);
        when(pengirimanRepository.findById(dummyId)).thenReturn(Optional.of(pengiriman));
        when(pengirimanRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        Pengiriman result = pengirimanService.tolakParsialOlehAdmin(dummyId, "Buah busuk sebagian", 250.0);
        assertEquals(StatusPengiriman.DITOLAK_PARSIAL_ADMIN, result.getStatus());
        assertEquals(250.0, result.getAngkutanDiakui());
        assertEquals("Buah busuk sebagian", result.getAlasanPenolakan());
    }
}