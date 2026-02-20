package com.mysawit.mysawit_pengiriman.service;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.repository.PengirimanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
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
        pengiriman.setNama("Bambang");
        pengiriman.setTotalAngkutan(300.0);
    }

    @Test
    void testCreate() {
        // Atur skenario mock: Jika repo disuruh save apapun, kembalikan objek 'pengiriman'
        when(pengirimanRepository.save(any(Pengiriman.class))).thenReturn(pengiriman);

        Pengiriman result = pengirimanService.create(pengiriman);

        assertEquals("Bambang", result.getNama());
        verify(pengirimanRepository, times(1)).save(pengiriman);
    }

    @Test
    void testFindAll() {
        when(pengirimanRepository.findAll()).thenReturn(Arrays.asList(pengiriman));

        List<Pengiriman> result = pengirimanService.findAll();

        assertEquals(1, result.size());
        verify(pengirimanRepository, times(1)).findAll();
    }

    @Test
    void testFindById_Found() {
        when(pengirimanRepository.findById(dummyId)).thenReturn(Optional.of(pengiriman));

        Pengiriman result = pengirimanService.findById(dummyId);

        assertNotNull(result);
        assertEquals(dummyId, result.getId());
    }

    @Test
    void testFindById_NotFound() {
        UUID randomId = UUID.randomUUID();
        when(pengirimanRepository.findById(randomId)).thenReturn(Optional.empty());

        // Pastikan melempar RuntimeException sesuai logika di service-mu
        assertThrows(RuntimeException.class, () -> pengirimanService.findById(randomId));
    }

    @Test
    void testUpdate() {
        Pengiriman updatedDetails = new Pengiriman();
        updatedDetails.setNama("Bambang Baru");
        updatedDetails.setTotalAngkutan(350.0);

        when(pengirimanRepository.findById(dummyId)).thenReturn(Optional.of(pengiriman));
        when(pengirimanRepository.save(any(Pengiriman.class))).thenReturn(pengiriman);

        Pengiriman result = pengirimanService.update(dummyId, updatedDetails);

        assertEquals("Bambang Baru", result.getNama());
        assertEquals(350.0, result.getTotalAngkutan());
    }

    @Test
    void testDelete() {
        when(pengirimanRepository.findById(dummyId)).thenReturn(Optional.of(pengiriman));

        pengirimanService.delete(dummyId);

        verify(pengirimanRepository, times(1)).delete(pengiriman);
    }
}