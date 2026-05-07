package com.mysawit.pengiriman.service;

import com.mysawit.pengiriman.dto.AdminReviewRequest;
import com.mysawit.pengiriman.dto.CreateShipmentRequest;
import com.mysawit.pengiriman.dto.DriverSummaryResponse;
import com.mysawit.pengiriman.dto.DriverStatusUpdateRequest;
import com.mysawit.pengiriman.dto.MandorReviewRequest;
import com.mysawit.pengiriman.dto.ShipmentQueryRequest;
import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.entity.Shipment;
import com.mysawit.pengiriman.enums.AdminReviewDecision;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.exception.AccessDeniedBusinessException;
import com.mysawit.pengiriman.exception.BusinessRuleViolationException;
import com.mysawit.pengiriman.exception.ResourceNotFoundException;
import com.mysawit.pengiriman.integration.dto.HarvestSummary;
import com.mysawit.pengiriman.integration.gateway.HarvestGateway;
import com.mysawit.pengiriman.integration.gateway.PaymentGateway;
import com.mysawit.pengiriman.integration.gateway.UserGateway;
import com.mysawit.pengiriman.mapper.ShipmentMapper;
import com.mysawit.pengiriman.repository.ShipmentRepository;
import com.mysawit.pengiriman.repository.ShipmentSpecifications;
import com.mysawit.pengiriman.usecase.ShipmentCommandUseCase;
import com.mysawit.pengiriman.usecase.ShipmentQueryUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ShipmentService implements ShipmentCommandUseCase, ShipmentQueryUseCase {

    private static final BigDecimal MAX_TRUCK_CAPACITY_KG = new BigDecimal("400");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ShipmentRepository shipmentRepository;
    private final ShipmentMapper shipmentMapper;
    private final UserGateway userGateway;
    private final HarvestGateway harvestGateway;
    private final PaymentGateway paymentGateway;

    public ShipmentService(
        ShipmentRepository shipmentRepository,
        ShipmentMapper shipmentMapper,
        UserGateway userGateway,
        HarvestGateway harvestGateway,
        PaymentGateway paymentGateway
    ) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentMapper = shipmentMapper;
        this.userGateway = userGateway;
        this.harvestGateway = harvestGateway;
        this.paymentGateway = paymentGateway;
    }

    @Override
    public ShipmentResponse createShipment(CreateShipmentRequest request) {
        if (request.harvestIds().isEmpty()) {
            throw new BusinessRuleViolationException("At least one harvest must be selected");
        }
        if (!userGateway.areMandorAndDriverInSameEstate(request.mandorId(), request.driverId())) {
            throw new AccessDeniedBusinessException("Mandor and driver must belong to the same estate");
        }

        List<HarvestSummary> harvests = harvestGateway.getApprovedHarvests(request.harvestIds());
        validateHarvestSelection(request.harvestIds(), harvests);

        BigDecimal totalWeight = harvests.stream()
            .map(HarvestSummary::weightKg)
            .reduce(ZERO, BigDecimal::add);

        if (totalWeight.compareTo(MAX_TRUCK_CAPACITY_KG) > 0) {
            throw new BusinessRuleViolationException("Shipment total weight cannot exceed 400 kg");
        }

        Shipment shipment = new Shipment();
        shipment.setMandorId(request.mandorId());
        shipment.setDriverId(request.driverId());
        shipment.setHarvestIds(request.harvestIds());
        shipment.setTotalWeightKg(totalWeight);
        shipment.setStatus(ShipmentStatus.MEMUAT);

        return shipmentMapper.toResponse(shipmentRepository.save(shipment));
    }

    @Override
    public ShipmentResponse updateDriverStatus(UUID shipmentId, DriverStatusUpdateRequest request) {
        Shipment shipment = getShipmentEntity(shipmentId);
        if (!shipment.getDriverId().equals(request.driverId())) {
            throw new AccessDeniedBusinessException("Driver is not assigned to this shipment");
        }

        validateDriverStatusTransition(shipment.getStatus(), request.newStatus());
        shipment.setStatus(request.newStatus());

        return shipmentMapper.toResponse(shipmentRepository.save(shipment));
    }

    @Override
    public ShipmentResponse reviewByMandor(UUID shipmentId, MandorReviewRequest request) {
        Shipment shipment = getShipmentEntity(shipmentId);
        if (!shipment.getMandorId().equals(request.mandorId())) {
            throw new AccessDeniedBusinessException("Mandor is not assigned to this shipment");
        }
        if (shipment.getStatus() != ShipmentStatus.TIBA_DI_TUJUAN) {
            throw new BusinessRuleViolationException("Shipment must arrive before mandor review");
        }

        shipment.setMandorReviewedAt(Instant.now());
        if (request.approved()) {
            shipment.setStatus(ShipmentStatus.DISETUJUI_MANDOR);
            shipment.setRejectionReason(null);
            paymentGateway.triggerDriverPayroll(
                shipment.getDriverId(),
                shipment.getId(),
                shipment.getTotalWeightKg()
            );
        } else {
            requireReason(request.rejectionReason(), "Mandor rejection reason is required");
            shipment.setStatus(ShipmentStatus.DITOLAK_MANDOR);
            shipment.setRejectionReason(request.rejectionReason().trim());
        }

        return shipmentMapper.toResponse(shipmentRepository.save(shipment));
    }

    @Override
    public ShipmentResponse reviewByAdmin(UUID shipmentId, AdminReviewRequest request) {
        if (!userGateway.isAdmin(request.adminId())) {
            throw new AccessDeniedBusinessException("Only admin can review shipment");
        }

        Shipment shipment = getShipmentEntity(shipmentId);
        if (shipment.getStatus() != ShipmentStatus.DISETUJUI_MANDOR) {
            throw new BusinessRuleViolationException("Shipment must be approved by mandor before admin review");
        }

        shipment.setAdminReviewedAt(Instant.now());
        switch (request.decision()) {
            case APPROVE -> approveByAdmin(shipment);
            case REJECT -> rejectByAdmin(shipment, request);
            case PARTIAL_REJECT -> partialRejectByAdmin(shipment, request);
            default -> throw new IllegalArgumentException("Unsupported admin review decision");
        }

        return shipmentMapper.toResponse(shipmentRepository.save(shipment));
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(UUID shipmentId) {
        return shipmentMapper.toResponse(getShipmentEntity(shipmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getShipments(ShipmentQueryRequest request) {
        Specification<Shipment> specification = Specification.allOf(
            ShipmentSpecifications.hasDriverId(request.driverId()),
            ShipmentSpecifications.hasMandorId(request.mandorId()),
            ShipmentSpecifications.hasStatus(request.status()),
            ShipmentSpecifications.createdOn(request.date())
        );

        return shipmentRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "createdAt"))
            .stream()
            .map(shipmentMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getShipmentsApprovedByMandor(ShipmentQueryRequest request) {
        ShipmentQueryRequest query = new ShipmentQueryRequest(
            request.driverId(),
            request.mandorId(),
            ShipmentStatus.DISETUJUI_MANDOR,
            request.date()
        );
        return getShipments(query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverSummaryResponse> getAvailableDriversForMandor(String mandorId, String search) {
        return userGateway.getDriversForMandor(mandorId, search)
            .stream()
            .map(driver -> new DriverSummaryResponse(driver.id(), driver.name(), driver.estateId()))
            .toList();
    }

    private Shipment getShipmentEntity(UUID shipmentId) {
        return shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment with id " + shipmentId + " was not found"));
    }

    private void validateHarvestSelection(List<String> requestedHarvestIds, List<HarvestSummary> harvests) {
        Set<String> foundHarvestIds = harvests.stream()
            .map(HarvestSummary::id)
            .collect(java.util.stream.Collectors.toSet());

        if (foundHarvestIds.size() != requestedHarvestIds.size()) {
            throw new BusinessRuleViolationException("Some harvest ids are invalid or unavailable");
        }

        boolean containsUnapprovedHarvest = harvests.stream().anyMatch(harvest -> !harvest.approved());
        if (containsUnapprovedHarvest) {
            throw new BusinessRuleViolationException("Only approved harvests can be included in a shipment");
        }
    }

    private void validateDriverStatusTransition(ShipmentStatus currentStatus, ShipmentStatus newStatus) {
        if (!currentStatus.isDriverMutable()) {
            throw new BusinessRuleViolationException("Shipment status can no longer be changed by driver");
        }
        boolean validTransition = (currentStatus == ShipmentStatus.MEMUAT && newStatus == ShipmentStatus.MENGIRIM)
            || (currentStatus == ShipmentStatus.MENGIRIM && newStatus == ShipmentStatus.TIBA_DI_TUJUAN);
        if (!validTransition) {
            throw new BusinessRuleViolationException(
                "Invalid shipment status transition from " + currentStatus + " to " + newStatus
            );
        }
    }

    private void approveByAdmin(Shipment shipment) {
        shipment.setStatus(ShipmentStatus.DISETUJUI_ADMIN);
        shipment.setRecognizedWeightKg(shipment.getTotalWeightKg());
        shipment.setRejectionReason(null);
        paymentGateway.triggerMandorPayroll(
            shipment.getMandorId(),
            shipment.getId(),
            shipment.getTotalWeightKg()
        );
    }

    private void rejectByAdmin(Shipment shipment, AdminReviewRequest request) {
        requireReason(request.rejectionReason(), "Admin rejection reason is required");
        shipment.setStatus(ShipmentStatus.DITOLAK_ADMIN);
        shipment.setRecognizedWeightKg(ZERO);
        shipment.setRejectionReason(request.rejectionReason().trim());
    }

    private void partialRejectByAdmin(Shipment shipment, AdminReviewRequest request) {
        requireReason(request.rejectionReason(), "Admin rejection reason is required");
        if (request.recognizedWeightKg() == null) {
            throw new BusinessRuleViolationException("Recognized weight is required for partial rejection");
        }
        BigDecimal recognizedWeight = request.recognizedWeightKg();
        if (recognizedWeight.compareTo(ZERO) <= 0
            || recognizedWeight.compareTo(shipment.getTotalWeightKg()) >= 0) {
            throw new BusinessRuleViolationException(
                "Recognized weight must be greater than 0 and lower than total shipment weight"
            );
        }

        shipment.setStatus(ShipmentStatus.DITOLAK_PARSIAL_ADMIN);
        shipment.setRecognizedWeightKg(recognizedWeight);
        shipment.setRejectionReason(request.rejectionReason().trim());
        paymentGateway.triggerMandorPayroll(
            shipment.getMandorId(),
            shipment.getId(),
            recognizedWeight
        );
    }

    private void requireReason(String reason, String message) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessRuleViolationException(message);
        }
    }
}
