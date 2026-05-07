package com.mysawit.pengiriman.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysawit.pengiriman.dto.DriverStatusUpdateRequest;
import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.proto.GetShipmentByIdRequest;
import com.mysawit.pengiriman.proto.ShipmentStatusGrpc;
import com.mysawit.pengiriman.proto.UpdateDriverStatusGrpcRequest;
import com.mysawit.pengiriman.usecase.ShipmentCommandUseCase;
import com.mysawit.pengiriman.usecase.ShipmentQueryUseCase;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipmentGrpcServiceTest {

    @Mock
    private ShipmentQueryUseCase shipmentQueryUseCase;

    @Mock
    private ShipmentCommandUseCase shipmentCommandUseCase;

    private ShipmentGrpcService shipmentGrpcService;

    @BeforeEach
    void setUp() {
        shipmentGrpcService = new ShipmentGrpcService(
            shipmentQueryUseCase,
            shipmentCommandUseCase,
            new ShipmentGrpcMapper()
        );
    }

    @Test
    void getShipmentByIdShouldMapResponseToGrpcMessage() {
        UUID shipmentId = UUID.randomUUID();
        when(shipmentQueryUseCase.getShipmentById(shipmentId)).thenReturn(buildResponse(shipmentId, ShipmentStatus.MEMUAT));
        RecordingObserver<com.mysawit.pengiriman.proto.ShipmentMessage> observer = new RecordingObserver<>();

        shipmentGrpcService.getShipmentById(
            GetShipmentByIdRequest.newBuilder().setShipmentId(shipmentId.toString()).build(),
            observer
        );

        assertEquals(shipmentId.toString(), observer.value.getId());
        assertEquals(ShipmentStatusGrpc.MEMUAT, observer.value.getStatus());
    }

    @Test
    void updateDriverStatusShouldMapGrpcEnumToDomainEnum() {
        UUID shipmentId = UUID.randomUUID();
        when(shipmentCommandUseCase.updateDriverStatus(any(), any()))
            .thenReturn(buildResponse(shipmentId, ShipmentStatus.MENGIRIM));
        RecordingObserver<com.mysawit.pengiriman.proto.ShipmentMessage> observer = new RecordingObserver<>();

        shipmentGrpcService.updateDriverStatus(
            UpdateDriverStatusGrpcRequest.newBuilder()
                .setShipmentId(shipmentId.toString())
                .setDriverId("driver-1")
                .setNewStatus(ShipmentStatusGrpc.MENGIRIM)
                .build(),
            observer
        );

        ArgumentCaptor<DriverStatusUpdateRequest> captor = ArgumentCaptor.forClass(DriverStatusUpdateRequest.class);
        verify(shipmentCommandUseCase).updateDriverStatus(org.mockito.ArgumentMatchers.eq(shipmentId), captor.capture());
        assertEquals(ShipmentStatus.MENGIRIM, captor.getValue().newStatus());
        assertEquals(ShipmentStatusGrpc.MENGIRIM, observer.value.getStatus());
    }

    private ShipmentResponse buildResponse(UUID shipmentId, ShipmentStatus status) {
        return new ShipmentResponse(
            shipmentId,
            "driver-1",
            "mandor-1",
            List.of("harvest-1"),
            new BigDecimal("150"),
            null,
            status,
            null,
            Instant.now(),
            Instant.now(),
            null,
            null
        );
    }

    private static final class RecordingObserver<T> implements StreamObserver<T> {

        private T value;

        @Override
        public void onNext(T value) {
            this.value = value;
        }

        @Override
        public void onError(Throwable throwable) {
            throw new AssertionError("Unexpected error", throwable);
        }

        @Override
        public void onCompleted() {
            // no-op
        }
    }
}
