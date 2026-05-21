package com.mysawit.pengiriman.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.pengiriman.dto.AdminReviewRequest;
import com.mysawit.pengiriman.dto.CreateShipmentRequest;
import com.mysawit.pengiriman.dto.DriverStatusUpdateRequest;
import com.mysawit.pengiriman.dto.MandorReviewRequest;
import com.mysawit.pengiriman.entity.Shipment;
import com.mysawit.pengiriman.enums.AdminReviewDecision;
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
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("POST /api/v1/shipments should create shipment")
    void createShipment() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest(
            "mandor-1", "driver-1", List.of("h1", "h2")
        );
        Mockito.when(userGateway.areMandorAndDriverInSameEstate("mandor-1", "driver-1"))
            .thenReturn(true);
        Mockito.when(harvestGateway.getApprovedHarvests(request.harvestIds()))
            .thenReturn(List.of(
                new HarvestSummary("h1", new BigDecimal("100"), true),
                new HarvestSummary("h2", new BigDecimal("120"), true)
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
    @DisplayName("PATCH driver-status should update MEMUAT -> MENGIRIM")
    void updateDriverStatus() throws Exception {
        Shipment s = shipmentRepository.save(
            buildShipment(ShipmentStatus.MEMUAT, "150"));
        DriverStatusUpdateRequest request =
            new DriverStatusUpdateRequest("driver-1", ShipmentStatus.MENGIRIM);

        mockMvc.perform(patch("/api/v1/shipments/{id}/driver-status", s.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("MENGIRIM"));
    }

    @Test
    @DisplayName("GET /api/v1/shipments should filter by driver and date")
    void getShipmentsFiltered() throws Exception {
        Shipment s = shipmentRepository.save(
            buildShipment(ShipmentStatus.MEMUAT, "130"));
        String date = s.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().toString();

        mockMvc.perform(get("/api/v1/shipments")
                .param("driverId", "driver-1")
                .param("date", date))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(s.getId().toString()));
    }

    @Test
    @DisplayName("GET /api/v1/shipments/{id} should return shipment by id")
    void getShipmentById() throws Exception {
        Shipment s = shipmentRepository.save(
            buildShipment(ShipmentStatus.MEMUAT, "130"));

        mockMvc.perform(get("/api/v1/shipments/{id}", s.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(s.getId().toString()));
    }

    @Test
    @DisplayName("GET /api/v1/shipments/{id} should return 404 when not found")
    void getShipmentByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/shipments/{id}",
                "00000000-0000-0000-0000-000000000099"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH mandor-review should approve shipment")
    void mandorApprove() throws Exception {
        Shipment s = shipmentRepository.save(
            buildShipment(ShipmentStatus.TIBA_DI_TUJUAN, "180"));
        MandorReviewRequest request =
            new MandorReviewRequest("mandor-1", true, null);

        mockMvc.perform(patch("/api/v1/shipments/{id}/mandor-review", s.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DISETUJUI_MANDOR"));
    }

    @Test
    @DisplayName("PATCH mandor-review should reject with reason")
    void mandorReject() throws Exception {
        Shipment s = shipmentRepository.save(
            buildShipment(ShipmentStatus.TIBA_DI_TUJUAN, "180"));
        MandorReviewRequest request =
            new MandorReviewRequest("mandor-1", false, "Kualitas buruk");

        mockMvc.perform(patch("/api/v1/shipments/{id}/mandor-review", s.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DITOLAK_MANDOR"))
            .andExpect(jsonPath("$.rejectionReason").value("Kualitas buruk"));
    }

    @Test
    @DisplayName("PATCH admin-review should approve shipment")
    void adminApprove() throws Exception {
        Shipment s = shipmentRepository.save(
            buildShipment(ShipmentStatus.DISETUJUI_MANDOR, "200"));
        Mockito.when(userGateway.isAdmin("admin-1")).thenReturn(true);
        AdminReviewRequest request =
            new AdminReviewRequest("admin-1", AdminReviewDecision.APPROVE, null, null);

        mockMvc.perform(patch("/api/v1/shipments/{id}/admin-review", s.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DISETUJUI_ADMIN"));
    }

    @Test
    @DisplayName("PATCH admin-review should partial reject with recognized weight")
    void adminPartialReject() throws Exception {
        Shipment s = shipmentRepository.save(
            buildShipment(ShipmentStatus.DISETUJUI_MANDOR, "200"));
        Mockito.when(userGateway.isAdmin("admin-1")).thenReturn(true);
        AdminReviewRequest request = new AdminReviewRequest(
            "admin-1", AdminReviewDecision.PARTIAL_REJECT,
            new BigDecimal("125"), "Sebagian kurang"
        );

        mockMvc.perform(patch("/api/v1/shipments/{id}/admin-review", s.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DITOLAK_PARSIAL_ADMIN"))
            .andExpect(jsonPath("$.recognizedWeightKg").value(125));
    }

    @Test
    @DisplayName("POST should return 400 when request body is invalid")
    void createShipmentValidationError() throws Exception {
        String invalidJson = "{\"mandorId\":\"\",\"driverId\":\"\",\"harvestIds\":[]}";

        mockMvc.perform(post("/api/v1/shipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/shipments/approved-by-mandor should return approved list")
    void getApprovedByMandor() throws Exception {
        Shipment s = shipmentRepository.save(
            buildShipment(ShipmentStatus.DISETUJUI_MANDOR, "200"));

        mockMvc.perform(get("/api/v1/shipments/approved-by-mandor")
                .param("mandorId", "mandor-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/shipments/drivers should return driver list")
    void getDrivers() throws Exception {
        Mockito.when(userGateway.getDriversForMandor("mandor-1", "john"))
            .thenReturn(List.of(
                new com.mysawit.pengiriman.integration.dto.DriverSummary(
                    "d1", "John", "estate-1"
                )
            ));

        mockMvc.perform(get("/api/v1/shipments/drivers")
                .param("mandorId", "mandor-1")
                .param("search", "john"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name").value("John"));
    }

    @Test
    @DisplayName("GET /api/v1/shipments with status filter")
    void getShipmentsWithStatusFilter() throws Exception {
        shipmentRepository.save(
            buildShipment(ShipmentStatus.MEMUAT, "130"));
        shipmentRepository.save(
            buildShipment(ShipmentStatus.MENGIRIM, "150"));

        mockMvc.perform(get("/api/v1/shipments")
                .param("status", "MEMUAT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    private Shipment buildShipment(ShipmentStatus status, String weight) {
        Shipment s = new Shipment();
        s.setDriverId("driver-1");
        s.setMandorId("mandor-1");
        s.setHarvestIds(new ArrayList<>(List.of("h1")));
        s.setStatus(status);
        s.setTotalWeightKg(new BigDecimal(weight));
        return s;
    }
}
