package com.mysawit.pengiriman.grpc;

import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.proto.RecognizedWeightResponse;
import com.mysawit.pengiriman.proto.ShipmentMessage;
import com.mysawit.pengiriman.proto.ShipmentStatusGrpc;
import com.mysawit.pengiriman.proto.ShipmentSummary;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ShipmentGrpcMapper {

    /**
     * Maps domain response to ShipmentSummary — the payment-compatible message.
     * Field aliases used by payment module:
     *   supir_user_id  ← driverId
     *   mandor_user_id ← mandorId
     *   delivered_kg   ← totalWeightKg
     *   recognized_kg  ← recognizedWeightKg (empty string if not yet reviewed)
     */
    public ShipmentSummary toSummary(ShipmentResponse response) {
        return ShipmentSummary.newBuilder()
            .setId(response.id().toString())
            .setStatus(response.status().name())
            .setSupirUserId(response.driverId())
            .setMandorUserId(response.mandorId())
            .setDeliveredKg(response.totalWeightKg().toPlainString())
            .setRecognizedKg(response.recognizedWeightKg() != null
                ? response.recognizedWeightKg().toPlainString()
                : "")
            .setCreatedAt(toString(response.createdAt()))
            .setMandorReviewedAt(toString(response.mandorReviewedAt()))
            .setAdminReviewedAt(toString(response.adminReviewedAt()))
            .build();
    }

    /**
     * Maps domain response to lightweight RecognizedWeightResponse.
     * Used by GetRecognizedWeight RPC — minimal payload for payroll Mandor.
     */
    public RecognizedWeightResponse toRecognizedWeight(ShipmentResponse response) {
        return RecognizedWeightResponse.newBuilder()
            .setShipmentId(response.id().toString())
            .setRecognizedKg(response.recognizedWeightKg() != null
                ? response.recognizedWeightKg().toPlainString()
                : "")
            .build();
    }

    /**
     * Maps domain response to full ShipmentMessage — for internal RPCs
     * (ListShipmentsByDriver, UpdateDriverStatus).
     */
    public ShipmentMessage toGrpc(ShipmentResponse response) {
        ShipmentMessage.Builder builder = ShipmentMessage.newBuilder()
            .setId(response.id().toString())
            .setDriverId(response.driverId())
            .setMandorId(response.mandorId())
            .addAllHarvestIds(response.harvestIds())
            .setTotalWeightKg(response.totalWeightKg().toPlainString())
            .setStatus(toGrpcStatus(response.status()))
            .setRejectionReason(valueOrEmpty(response.rejectionReason()))
            .setCreatedAt(toString(response.createdAt()))
            .setUpdatedAt(toString(response.updatedAt()))
            .setMandorReviewedAt(toString(response.mandorReviewedAt()))
            .setAdminReviewedAt(toString(response.adminReviewedAt()));

        if (response.recognizedWeightKg() != null) {
            builder.setRecognizedWeightKg(response.recognizedWeightKg().toPlainString());
        }
        return builder.build();
    }

    public ShipmentStatusGrpc toGrpcStatus(ShipmentStatus status) {
        return ShipmentStatusGrpc.valueOf(status.name());
    }

    public ShipmentStatus toDomainStatus(ShipmentStatusGrpc statusGrpc) {
        if (statusGrpc == ShipmentStatusGrpc.SHIPMENT_STATUS_UNSPECIFIED) {
            throw new IllegalArgumentException("Shipment status is required");
        }
        return ShipmentStatus.valueOf(statusGrpc.name());
    }

    public BigDecimal parseWeight(String weight) {
        return new BigDecimal(weight);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String toString(Instant instant) {
        return instant == null ? "" : instant.toString();
    }
}
