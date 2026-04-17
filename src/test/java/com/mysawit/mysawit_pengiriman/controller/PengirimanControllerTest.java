package com.mysawit.mysawit_pengiriman.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.mysawit_pengiriman.dto.AssignPengirimanRequest;
import com.mysawit.mysawit_pengiriman.dto.UpdateStatusRequest;
import com.mysawit.mysawit_pengiriman.dto.VerifikasiRequest;
import com.mysawit.mysawit_pengiriman.exception.PengirimanValidationException;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
        dummyPengiriman.setSupirNama("Budi");
        dummyPengiriman.setMandorNama("Rahmat");
        dummyPengiriman.setHasilPanenIds(List.of(UUID.randomUUID()));
    }

    @Test
    void testAssignPengiriman_Success() throws Exception {
        AssignPengirimanRequest request = new AssignPengirimanRequest();
        request.setSupirId(UUID.randomUUID());
        request.setMandorId(UUID.randomUUID());
        request.setSupirNama("Budi");
        request.setMandorNama("Rahmat");
        request.setTotalAngkutan(400.0);
        request.setHasilPanenIds(List.of(UUID.randomUUID()));

        when(pengirimanService.assignPengiriman(any(Pengiriman.class))).thenReturn(dummyPengiriman);

        mockMvc.perform(post("/api/pengiriman/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("MEMUAT"))
            .andExpect(jsonPath("$.totalAngkutan").value(400.0))
            .andExpect(jsonPath("$.supirNama").value("Budi"));
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
    void testGetDaftarPersetujuanAdmin_Success() throws Exception {
        dummyPengiriman.setStatus(StatusPengiriman.DISETUJUI_MANDOR);

        when(pengirimanService.getDaftarPersetujuanAdmin("rah", null))
            .thenReturn(List.of(dummyPengiriman));

        mockMvc.perform(get("/api/pengiriman/admin/persetujuan")
                .param("mandorNama", "rah"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].mandorNama").value("Rahmat"))
            .andExpect(jsonPath("$[0].status").value("DISETUJUI_MANDOR"));
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

    @Test
    void testAssignPengiriman_InvalidRequest_ReturnsBadRequest() throws Exception {
        AssignPengirimanRequest request = new AssignPengirimanRequest();
        request.setSupirId(UUID.randomUUID());
        request.setMandorId(UUID.randomUUID());

        mockMvc.perform(post("/api/pengiriman/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Request tidak valid"));
    }

    @Test
    void testUpdateStatusSupir_ServiceValidation_ReturnsBadRequest() throws Exception {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(StatusPengiriman.TIBA_DI_TUJUAN);

        doThrow(new PengirimanValidationException("Supir tidak memiliki akses untuk menetapkan status ini"))
            .when(pengirimanService).updateStatusBySupir(dummyId, StatusPengiriman.TIBA_DI_TUJUAN);

        mockMvc.perform(patch("/api/pengiriman/" + dummyId + "/status-supir")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Supir tidak memiliki akses untuk menetapkan status ini"));
    }
}
