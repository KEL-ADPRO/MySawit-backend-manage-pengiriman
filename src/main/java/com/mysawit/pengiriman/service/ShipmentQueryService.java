package com.mysawit.pengiriman.service;

import com.mysawit.pengiriman.dto.DriverSummaryResponse;
import com.mysawit.pengiriman.dto.ShipmentQueryRequest;
import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.entity.Shipment;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.exception.ResourceNotFoundException;
import com.mysawit.pengiriman.integration.gateway.UserGateway;
import com.mysawit.pengiriman.mapper.ShipmentMapper;
import com.mysawit.pengiriman.repository.ShipmentRepository;
import com.mysawit.pengiriman.repository.ShipmentSpecifications;
import com.mysawit.pengiriman.usecase.ShipmentQueryUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles all shipment read operations (SRP: query-only).
 */
@Service
@Transactional(readOnly = true)
public class ShipmentQueryService implements ShipmentQueryUseCase {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentMapper shipmentMapper;
    private final UserGateway userGateway;

    public ShipmentQueryService(
        ShipmentRepository shipmentRepository,
        ShipmentMapper shipmentMapper,
        UserGateway userGateway
    ) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentMapper = shipmentMapper;
        this.userGateway = userGateway;
    }

    @Override
    public ShipmentResponse getShipmentById(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Shipment with id " + shipmentId + " was not found"
            ));
        return shipmentMapper.toResponse(shipment);
    }

    @Override
    public List<ShipmentResponse> getShipments(ShipmentQueryRequest request) {
        Specification<Shipment> spec = Specification
            .where(ShipmentSpecifications.hasDriverId(request.driverId()))
            .and(ShipmentSpecifications.hasMandorId(request.mandorId()))
            .and(ShipmentSpecifications.hasStatus(request.status()))
            .and(ShipmentSpecifications.createdOn(request.date()));

        return shipmentRepository
            .findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"))
            .stream()
            .map(shipmentMapper::toResponse)
            .toList();
    }

    @Override
    public List<ShipmentResponse> getShipmentsApprovedByMandor(
        ShipmentQueryRequest request
    ) {
        ShipmentQueryRequest query = new ShipmentQueryRequest(
            request.driverId(),
            request.mandorId(),
            ShipmentStatus.DISETUJUI_MANDOR,
            request.date()
        );
        return getShipments(query);
    }

    @Override
    public List<DriverSummaryResponse> getAvailableDriversForMandor(
        String mandorId, String search
    ) {
        return userGateway.getDriversForMandor(mandorId, search)
            .stream()
            .map(d -> new DriverSummaryResponse(d.id(), d.name(), d.estateId()))
            .toList();
    }
}
