package com.mysawit.pengiriman.grpc;

import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.proto.ShipmentMessage;
import com.mysawit.pengiriman.proto.ShipmentStatusGrpc;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ShipmentGrpcMapper {

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
