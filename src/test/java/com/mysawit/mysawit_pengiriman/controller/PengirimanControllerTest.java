package com.mysawit.mysawit_pengiriman.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.mysawit_pengiriman.dto.AssignPengirimanRequest;
import com.mysawit.mysawit_pengiriman.dto.UpdateStatusRequest;
import com.mysawit.mysawit_pengiriman.dto.VerifikasiRequest;
import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.model.StatusPengiriman;
import com.mysawit.mysawit_pengiriman.service.PengirimanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PengirimanController.class)
class PengirimanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PengirimanService pengirimanService;

    private UUID dummyId;
    private Pengiriman dummyPengiriman;

    @BeforeEach
    void setUp() {
        dummyId = UUID.randomUUID();
        dummyPengiriman = new Pengiriman();
        dummyPengiriman.setId(dummyId);
        dummyPengiriman.setStatus(StatusPengiriman.MEMUAT);
        dummyPengiriman.setTotalAngkutan(400.0);
    }

    @Test
    void testAssignPengiriman_Success() throws Exception {
        AssignPengirimanRequest request = new AssignPengirimanRequest();
        request.setSupirId(UUID.randomUUID());
        request.setMandorId(UUID.randomUUID());
        request.setTotalAngkutan(400.0);

        when(pengirimanService.assignPengiriman(any(Pengiriman.class))).thenReturn(dummyPengiriman);

        mockMvc.perform(post("/api/pengiriman/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("MEMUAT"))
            .andExpect(jsonPath("$.totalAngkutan").value(400.0));
    }

    @Test
    void testUpdateStatusSupir_Success() throws Exception {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(StatusPengiriman.TIBA_DI_TUJUAN);

        dummyPengiriman.setStatus(StatusPengiriman.TIBA_DI_TUJUAN);
        when(pengirimanService.updateStatusBySupir(eq(dummyId), any(StatusPengiriman.class)))
            .thenReturn(dummyPengiriman);

        mockMvc.perform(patch("/api/pengiriman/" + dummyId + "/status-supir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("TIBA_DI_TUJUAN"));
    }

    @Test
    void testVerifikasiMandor_Reject_Success() throws Exception {
        VerifikasiRequest request = new VerifikasiRequest();
        request.setApproved(false);
        request.setAlasan("Buah rusak di jalan");

        dummyPengiriman.setStatus(StatusPengiriman.DITOLAK_MANDOR);
        dummyPengiriman.setAlasanPenolakan("Buah rusak di jalan");
        when(pengirimanService.verifikasiOlehMandor(dummyId, false, "Buah rusak di jalan"))
            .thenReturn(dummyPengiriman);

        mockMvc.perform(post("/api/pengiriman/" + dummyId + "/verifikasi-mandor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DITOLAK_MANDOR"))
            .andExpect(jsonPath("$.alasanPenolakan").value("Buah rusak di jalan"));
    }
}