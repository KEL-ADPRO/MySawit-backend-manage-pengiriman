package com.mysawit.pengiriman.service;

import com.mysawit.pengiriman.dto.AdminReviewRequest;
import com.mysawit.pengiriman.dto.CreateShipmentRequest;
import com.mysawit.pengiriman.dto.DriverStatusUpdateRequest;
import com.mysawit.pengiriman.dto.MandorReviewRequest;
import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.entity.Shipment;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.event.ShipmentApprovedByAdminEvent;
import com.mysawit.pengiriman.event.ShipmentApprovedByMandorEvent;
import com.mysawit.pengiriman.event.ShipmentPartialRejectedByAdminEvent;
import com.mysawit.pengiriman.exception.AccessDeniedBusinessException;
import com.mysawit.pengiriman.exception.BusinessRuleViolationException;
import com.mysawit.pengiriman.exception.ResourceNotFoundException;
import com.mysawit.pengiriman.integration.dto.HarvestSummary;
import com.mysawit.pengiriman.integration.gateway.HarvestGateway;
import com.mysawit.pengiriman.integration.gateway.UserGateway;
import com.mysawit.pengiriman.mapper.ShipmentMapper;
import com.mysawit.pengiriman.repository.ShipmentRepository;
import com.mysawit.pengiriman.service.strategy.AdminReviewStrategy;
import com.mysawit.pengiriman.service.strategy.AdminReviewStrategyFactory;
import com.mysawit.pengiriman.usecase.ShipmentCommandUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mysawit.pengiriman.profiling.Profiled;
import org.springframework.util.StringUtils;

/**
 * Handles all shipment write operations (SRP: command-only).
 * Uses Strategy Pattern for admin reviews and Observer Pattern for payroll events.
 */
@Profiled(category = "shipment.command")
@Service
@Transactional
public class ShipmentCommandService implements ShipmentCommandUseCase {

    private static final BigDecimal MAX_TRUCK_CAPACITY_KG = new BigDecimal("400");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ShipmentRepository shipmentRepository;
    private final ShipmentMapper shipmentMapper;
    private final UserGateway userGateway;
    private final HarvestGateway harvestGateway;
    private final AdminReviewStrategyFactory adminReviewStrategyFactory;
    private final ApplicationEventPublisher eventPublisher;

    public ShipmentCommandService(
        ShipmentRepository shipmentRepository,
        ShipmentMapper shipmentMapper,
        UserGateway userGateway,
        HarvestGateway harvestGateway,
        AdminReviewStrategyFactory adminReviewStrategyFactory,
        ApplicationEventPublisher eventPublisher
    ) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentMapper = shipmentMapper;
        this.userGateway = userGateway;
        this.harvestGateway = harvestGateway;
        this.adminReviewStrategyFactory = adminReviewStrategyFactory;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ShipmentResponse createShipment(CreateShipmentRequest request) {
        if (request.harvestIds().isEmpty()) {
            throw new BusinessRuleViolationException(
                "At least one harvest must be selected"
            );
        }
        if (!userGateway.areMandorAndDriverInSameEstate(
            request.mandorId(), request.driverId())) {
            throw new AccessDeniedBusinessException(
                "Mandor and driver must belong to the same estate"
            );
        }

        List<HarvestSummary> harvests =
            harvestGateway.getApprovedHarvests(request.harvestIds());
        validateHarvestSelection(request.harvestIds(), harvests);

        BigDecimal totalWeight = harvests.stream()
            .map(HarvestSummary::weightKg)
            .reduce(ZERO, BigDecimal::add);

        if (totalWeight.compareTo(MAX_TRUCK_CAPACITY_KG) > 0) {
            throw new BusinessRuleViolationException(
                "Shipment total weight cannot exceed 400 kg"
            );
        }

        Shipment shipment = new Shipment();
        shipment.setMandorId(request.mandorId());
        shipment.setDriverId(request.driverId());
        shipment.setHarvestIds(new ArrayList<>(request.harvestIds()));
        shipment.setTotalWeightKg(totalWeight);
        shipment.setStatus(ShipmentStatus.MEMUAT);

        return shipmentMapper.toResponse(shipmentRepository.save(shipment));
    }

    @Override
    public ShipmentResponse updateDriverStatus(
        UUID shipmentId, DriverStatusUpdateRequest request
    ) {
        Shipment shipment = findShipmentOrThrow(shipmentId);
        if (!shipment.getDriverId().equals(request.driverId())) {
            throw new AccessDeniedBusinessException(
                "Driver is not assigned to this shipment"
            );
        }

        if (!userGateway.areMandorAndDriverInSameEstate(
            shipment.getMandorId(), request.driverId())) {
        throw new AccessDeniedBusinessException(
            "Driver is not managed by the mandor of this shipment"
        );
    }

        ShipmentStatus current = shipment.getStatus();
        ShipmentStatus target = request.newStatus();

        if (!current.canTransitionTo(target)) {
            throw new BusinessRuleViolationException(
                "Invalid shipment status transition from " + current + " to " + target
            );
        }

        shipment.setStatus(target);
        return shipmentMapper.toResponse(shipmentRepository.save(shipment));
    }

    @Override
    public ShipmentResponse reviewByMandor(
        UUID shipmentId, MandorReviewRequest request
    ) {
        Shipment shipment = findShipmentOrThrow(shipmentId);
        if (!shipment.getMandorId().equals(request.mandorId())) {
            throw new AccessDeniedBusinessException(
                "Mandor is not assigned to this shipment"
            );
        }
        if (shipment.getStatus() != ShipmentStatus.TIBA_DI_TUJUAN) {
            throw new BusinessRuleViolationException(
                "Shipment must arrive before mandor review"
            );
        }

        shipment.setMandorReviewedAt(Instant.now());
        if (request.approved()) {
            shipment.setStatus(ShipmentStatus.DISETUJUI_MANDOR);
            shipment.setRejectionReason(null);
        } else {
            requireReason(request.rejectionReason(),
                "Mandor rejection reason is required");
            shipment.setStatus(ShipmentStatus.DITOLAK_MANDOR);
            shipment.setRejectionReason(request.rejectionReason().trim());
        }

        Shipment saved = shipmentRepository.save(shipment);

        if (saved.getStatus() == ShipmentStatus.DISETUJUI_MANDOR) {
            eventPublisher.publishEvent(new ShipmentApprovedByMandorEvent(
                saved.getDriverId(), saved.getId(), saved.getTotalWeightKg()
            ));
        }

        return shipmentMapper.toResponse(saved);
    }

    @Override
    public ShipmentResponse reviewByAdmin(
        UUID shipmentId, AdminReviewRequest request
    ) {
        if (!userGateway.isAdmin(request.adminId())) {
            throw new AccessDeniedBusinessException(
                "Only admin can review shipment"
            );
        }

        Shipment shipment = findShipmentOrThrow(shipmentId);
        if (shipment.getStatus() != ShipmentStatus.DISETUJUI_MANDOR) {
            throw new BusinessRuleViolationException(
                "Shipment must be approved by mandor before admin review"
            );
        }

        shipment.setAdminReviewedAt(Instant.now());

        // Strategy Pattern: delegate review logic to strategy
        AdminReviewStrategy strategy =
            adminReviewStrategyFactory.resolve(request.decision());
        strategy.execute(shipment, request);

        Shipment saved = shipmentRepository.save(shipment);
        publishAdminReviewEvents(saved);

        return shipmentMapper.toResponse(saved);
    }

    private void publishAdminReviewEvents(Shipment shipment) {
        if (shipment.getStatus() == ShipmentStatus.DISETUJUI_ADMIN) {
            eventPublisher.publishEvent(new ShipmentApprovedByAdminEvent(
                shipment.getMandorId(), shipment.getId(),
                shipment.getRecognizedWeightKg()
            ));
        } else if (shipment.getStatus() == ShipmentStatus.DITOLAK_PARSIAL_ADMIN) {
            eventPublisher.publishEvent(new ShipmentPartialRejectedByAdminEvent(
                shipment.getMandorId(), shipment.getId(),
                shipment.getRecognizedWeightKg()
            ));
        }
    }

    private Shipment findShipmentOrThrow(UUID shipmentId) {
        return shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Shipment with id " + shipmentId + " was not found"
            ));
    }

    private void validateHarvestSelection(
        List<String> requestedHarvestIds, List<HarvestSummary> harvests
    ) {
        Set<String> foundIds = harvests.stream()
            .map(HarvestSummary::id)
            .collect(Collectors.toSet());

        if (foundIds.size() != requestedHarvestIds.size()) {
            throw new BusinessRuleViolationException(
                "Some harvest ids are invalid or unavailable"
            );
        }

        boolean hasUnapproved = harvests.stream()
            .anyMatch(h -> !h.approved());
        if (hasUnapproved) {
            throw new BusinessRuleViolationException(
                "Only approved harvests can be included in a shipment"
            );
        }
    }

    private void requireReason(String reason, String message) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessRuleViolationException(message);
        }
    }
}
