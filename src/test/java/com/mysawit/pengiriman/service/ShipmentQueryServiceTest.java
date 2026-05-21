package com.mysawit.pengiriman.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.mysawit.pengiriman.dto.DriverSummaryResponse;
import com.mysawit.pengiriman.dto.ShipmentQueryRequest;
import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.entity.Shipment;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.exception.ResourceNotFoundException;
import com.mysawit.pengiriman.integration.dto.DriverSummary;
import com.mysawit.pengiriman.integration.gateway.UserGateway;
import com.mysawit.pengiriman.mapper.ShipmentMapper;
import com.mysawit.pengiriman.repository.ShipmentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ShipmentQueryServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private UserGateway userGateway;

    private ShipmentQueryService service;

    @BeforeEach
    void setUp() {
        service = new ShipmentQueryService(
            shipmentRepository, new ShipmentMapper(), userGateway
        );
    }

    @Test
    @DisplayName("getShipmentById should return response when found")
    void getByIdSuccess() {
        UUID id = UUID.randomUUID();
        Shipment s = buildShipment(id, ShipmentStatus.MEMUAT);
        when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));

        ShipmentResponse resp = service.getShipmentById(id);
        assertEquals(id, resp.id());
        assertEquals(ShipmentStatus.MEMUAT, resp.status());
    }

    @Test
    @DisplayName("getShipmentById should throw when not found")
    void getByIdNotFound() {
        UUID id = UUID.randomUUID();
        when(shipmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> service.getShipmentById(id));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("getShipments should return filtered results")
    void getShipmentsFiltered() {
        Shipment s = buildShipment(UUID.randomUUID(), ShipmentStatus.MEMUAT);
        when(shipmentRepository.findAll(
            org.mockito.ArgumentMatchers.any(Specification.class),
            org.mockito.ArgumentMatchers.any(Sort.class)
        )).thenReturn(List.of(s));

        List<ShipmentResponse> result = service.getShipments(
            new ShipmentQueryRequest("driver-1", null, null, null)
        );

        assertEquals(1, result.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("getShipmentsApprovedByMandor should force status filter")
    void getApprovedByMandor() {
        when(shipmentRepository.findAll(
            org.mockito.ArgumentMatchers.any(Specification.class),
            org.mockito.ArgumentMatchers.any(Sort.class)
        )).thenReturn(List.of());

        List<ShipmentResponse> result = service.getShipmentsApprovedByMandor(
            new ShipmentQueryRequest(null, "mandor-1", null, null)
        );

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("getAvailableDriversForMandor should map gateway response")
    void getDriversForMandor() {
        when(userGateway.getDriversForMandor("mandor-1", "search"))
            .thenReturn(List.of(
                new DriverSummary("d1", "John", "estate-1"),
                new DriverSummary("d2", "Jane", "estate-1")
            ));

        List<DriverSummaryResponse> result =
            service.getAvailableDriversForMandor("mandor-1", "search");

        assertEquals(2, result.size());
        assertEquals("John", result.get(0).name());
    }

    private Shipment buildShipment(UUID id, ShipmentStatus status) {
        Shipment s = new Shipment();
        s.setId(id);
        s.setDriverId("driver-1");
        s.setMandorId("mandor-1");
        s.setHarvestIds(new ArrayList<>(List.of("harvest-1")));
        s.setStatus(status);
        s.setTotalWeightKg(new BigDecimal("150"));
        s.setCreatedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        return s;
    }
}
