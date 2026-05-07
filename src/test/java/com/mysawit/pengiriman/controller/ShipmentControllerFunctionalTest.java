package com.mysawit.pengiriman.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.pengiriman.dto.CreateShipmentRequest;
import com.mysawit.pengiriman.dto.DriverStatusUpdateRequest;
import com.mysawit.pengiriman.entity.Shipment;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.integration.dto.HarvestSummary;
import com.mysawit.pengiriman.integration.gateway.HarvestGateway;
import com.mysawit.pengiriman.integration.gateway.PaymentGateway;
import com.mysawit.pengiriman.integration.gateway.UserGateway;
import com.mysawit.pengiriman.repository.ShipmentRepository;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ShipmentControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @MockBean
    private UserGateway userGateway;

    @MockBean
    private HarvestGateway harvestGateway;

    @MockBean
    private PaymentGateway paymentGateway;

    @BeforeEach
    void setUp() {
        shipmentRepository.deleteAll();
        Mockito.reset(userGateway, harvestGateway, paymentGateway);
    }

    @Test
    void createShipmentShouldReturnCreatedResponse() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest(
            "mandor-1",
            "driver-1",
            List.of("harvest-1", "harvest-2")
        );
        Mockito.when(userGateway.areMandorAndDriverInSameEstate("mandor-1", "driver-1")).thenReturn(true);
        Mockito.when(harvestGateway.getApprovedHarvests(request.harvestIds())).thenReturn(List.of(
            new HarvestSummary("harvest-1", new BigDecimal("100"), true),
            new HarvestSummary("harvest-2", new BigDecimal("120"), true)
        ));

        mockMvc.perform(post("/api/v1/shipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.driverId").value("driver-1"))
            .andExpect(jsonPath("$.mandorId").value("mandor-1"))
            .andExpect(jsonPath("$.status").value("MEMUAT"))
            .andExpect(jsonPath("$.totalWeightKg").value(220));
    }

    @Test
    void updateDriverStatusShouldReturnUpdatedShipment() throws Exception {
        Shipment shipment = buildShipment(ShipmentStatus.MEMUAT, new BigDecimal("150"));
        shipment = shipmentRepository.save(shipment);

        DriverStatusUpdateRequest request = new DriverStatusUpdateRequest("driver-1", ShipmentStatus.MENGIRIM);

        mockMvc.perform(patch("/api/v1/shipments/{shipmentId}/driver-status", shipment.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(shipment.getId().toString()))
            .andExpect(jsonPath("$.status").value("MENGIRIM"));
    }

    @Test
    void getShipmentsShouldFilterByDriverAndDate() throws Exception {
        Shipment shipment = shipmentRepository.save(buildShipment(ShipmentStatus.MEMUAT, new BigDecimal("130")));
        String createdDate = shipment.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().toString();

        mockMvc.perform(get("/api/v1/shipments")
                .param("driverId", "driver-1")
                .param("date", createdDate))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(shipment.getId().toString()));
    }

    private Shipment buildShipment(ShipmentStatus status, BigDecimal totalWeightKg) {
        Shipment shipment = new Shipment();
        shipment.setDriverId("driver-1");
        shipment.setMandorId("mandor-1");
        shipment.setHarvestIds(new ArrayList<>(List.of("harvest-1")));
        shipment.setStatus(status);
        shipment.setTotalWeightKg(totalWeightKg);
        return shipment;
    }
}
