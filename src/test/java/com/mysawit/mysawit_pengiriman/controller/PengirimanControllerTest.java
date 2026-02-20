package com.mysawit.mysawit_pengiriman.controller;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.service.PengirimanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PengirimanController.class)
class PengirimanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PengirimanService pengirimanService;

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
    void testGetAllPengiriman() throws Exception {
        when(pengirimanService.findAll()).thenReturn(Arrays.asList(pengiriman));

        mockMvc.perform(get("/api/pengiriman"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nama").value("Bambang"))
            .andExpect(jsonPath("$[0].totalAngkutan").value(300.0));
    }

    @Test
    void testGetPengirimanById() throws Exception {
        when(pengirimanService.findById(dummyId)).thenReturn(pengiriman);

        mockMvc.perform(get("/api/pengiriman/" + dummyId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nama").value("Bambang"));
    }

    @Test
    void testCreatePengiriman() throws Exception {
        when(pengirimanService.create(any(Pengiriman.class))).thenReturn(pengiriman);

        String jsonRequest = "{ \"nama\": \"Bambang\", \"totalAngkutan\": 300.0 }";

        mockMvc.perform(post("/api/pengiriman")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nama").value("Bambang"));
    }

    @Test
    void testUpdatePengiriman() throws Exception {
        when(pengirimanService.update(eq(dummyId), any(Pengiriman.class))).thenReturn(pengiriman);

        String jsonRequest = "{ \"nama\": \"Bambang Baru\", \"totalAngkutan\": 400.0 }";

        mockMvc.perform(put("/api/pengiriman/" + dummyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isOk());
    }

    @Test
    void testDeletePengiriman() throws Exception {
        doNothing().when(pengirimanService).delete(dummyId);

        String expectedResponse = "Data Pengiriman dengan ID " + dummyId + " berhasil dihapus.";

        mockMvc.perform(delete("/api/pengiriman/" + dummyId))
            .andExpect(status().isOk())
            .andExpect(content().string(expectedResponse));
    }
}