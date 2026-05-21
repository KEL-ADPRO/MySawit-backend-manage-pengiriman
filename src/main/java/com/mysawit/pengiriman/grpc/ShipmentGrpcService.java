package com.mysawit.pengiriman.grpc;

import com.mysawit.pengiriman.dto.DriverStatusUpdateRequest;
import com.mysawit.pengiriman.dto.ShipmentQueryRequest;
import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.proto.GetShipmentByIdRequest;
import com.mysawit.pengiriman.proto.ListShipmentsByDriverRequest;
import com.mysawit.pengiriman.proto.RecognizedWeightResponse;
import com.mysawit.pengiriman.proto.ShipmentGrpcServiceGrpc;
import com.mysawit.pengiriman.proto.ShipmentListMessage;
import com.mysawit.pengiriman.proto.ShipmentMessage;
import com.mysawit.pengiriman.proto.ShipmentSummary;
import com.mysawit.pengiriman.proto.UpdateDriverStatusGrpcRequest;
import com.mysawit.pengiriman.usecase.ShipmentCommandUseCase;
import com.mysawit.pengiriman.usecase.ShipmentQueryUseCase;
import io.grpc.stub.StreamObserver;
import java.time.LocalDate;
import java.util.UUID;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC server for the Shipment module.
 *
 * <p>GetShipmentById and GetRecognizedWeight are designed for the Payment module
 * to verify payroll source_id and retrieve billing weights.
 */
@GrpcService
public class ShipmentGrpcService extends ShipmentGrpcServiceGrpc.ShipmentGrpcServiceImplBase {

    private final ShipmentQueryUseCase shipmentQueryUseCase;
    private final ShipmentCommandUseCase shipmentCommandUseCase;
    private final ShipmentGrpcMapper shipmentGrpcMapper;

    public ShipmentGrpcService(
        ShipmentQueryUseCase shipmentQueryUseCase,
        ShipmentCommandUseCase shipmentCommandUseCase,
        ShipmentGrpcMapper shipmentGrpcMapper
    ) {
        this.shipmentQueryUseCase = shipmentQueryUseCase;
        this.shipmentCommandUseCase = shipmentCommandUseCase;
        this.shipmentGrpcMapper = shipmentGrpcMapper;
    }

    /**
     * Returns a ShipmentSummary with payment-compatible field names:
     *   supir_user_id, mandor_user_id, delivered_kg, recognized_kg.
     * Used by Payment module to verify payroll source and billing weights.
     */
    @Override
    public void getShipmentById(
        GetShipmentByIdRequest request,
        StreamObserver<ShipmentSummary> responseObserver
    ) {
        ShipmentResponse response = shipmentQueryUseCase
            .getShipmentById(UUID.fromString(request.getShipmentId()));
        responseObserver.onNext(shipmentGrpcMapper.toSummary(response));
        responseObserver.onCompleted();
    }

    /**
     * Lightweight RPC: returns only recognized_kg for a given shipment.
     * Payment module uses this when computing Mandor payroll amount.
     * recognized_kg = "" when Admin has not reviewed yet.
     */
    @Override
    public void getRecognizedWeight(
        GetShipmentByIdRequest request,
        StreamObserver<RecognizedWeightResponse> responseObserver
    ) {
        ShipmentResponse response = shipmentQueryUseCase
            .getShipmentById(UUID.fromString(request.getShipmentId()));
        responseObserver.onNext(shipmentGrpcMapper.toRecognizedWeight(response));
        responseObserver.onCompleted();
    }

    @Override
    public void listShipmentsByDriver(
        ListShipmentsByDriverRequest request,
        StreamObserver<ShipmentListMessage> responseObserver
    ) {
        LocalDate date = request.getDate().isBlank() ? null : LocalDate.parse(request.getDate());
        ShipmentListMessage response = ShipmentListMessage.newBuilder()
            .addAllShipments(
                shipmentQueryUseCase
                    .getShipments(new ShipmentQueryRequest(request.getDriverId(), null, null, date))
                    .stream()
                    .map(shipmentGrpcMapper::toGrpc)
                    .toList()
            )
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateDriverStatus(
        UpdateDriverStatusGrpcRequest request,
        StreamObserver<ShipmentMessage> responseObserver
    ) {
        ShipmentResponse response = shipmentCommandUseCase.updateDriverStatus(
            UUID.fromString(request.getShipmentId()),
            new DriverStatusUpdateRequest(
                request.getDriverId(),
                shipmentGrpcMapper.toDomainStatus(request.getNewStatus())
            )
        );
        responseObserver.onNext(shipmentGrpcMapper.toGrpc(response));
        responseObserver.onCompleted();
    }
}
