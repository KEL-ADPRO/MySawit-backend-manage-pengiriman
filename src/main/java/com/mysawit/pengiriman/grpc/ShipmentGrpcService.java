package com.mysawit.pengiriman.grpc;

import com.mysawit.pengiriman.dto.DriverStatusUpdateRequest;
import com.mysawit.pengiriman.dto.ShipmentQueryRequest;
import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.proto.GetShipmentByIdRequest;
import com.mysawit.pengiriman.proto.ListShipmentsByDriverRequest;
import com.mysawit.pengiriman.proto.ShipmentGrpcServiceGrpc;
import com.mysawit.pengiriman.proto.ShipmentListMessage;
import com.mysawit.pengiriman.proto.UpdateDriverStatusGrpcRequest;
import com.mysawit.pengiriman.usecase.ShipmentCommandUseCase;
import com.mysawit.pengiriman.usecase.ShipmentQueryUseCase;
import io.grpc.stub.StreamObserver;
import java.time.LocalDate;
import java.util.UUID;
import net.devh.boot.grpc.server.service.GrpcService;

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

    @Override
    public void getShipmentById(
        GetShipmentByIdRequest request,
        StreamObserver<com.mysawit.pengiriman.proto.ShipmentMessage> responseObserver
    ) {
        ShipmentResponse response = shipmentQueryUseCase.getShipmentById(UUID.fromString(request.getShipmentId()));
        responseObserver.onNext(shipmentGrpcMapper.toGrpc(response));
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
                shipmentQueryUseCase.getShipments(new ShipmentQueryRequest(request.getDriverId(), null, null, date))
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
        StreamObserver<com.mysawit.pengiriman.proto.ShipmentMessage> responseObserver
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
