package com.mysawit.pengiriman.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysawit.pengiriman.dto.DriverStatusUpdateRequest;
import com.mysawit.pengiriman.dto.ShipmentQueryRequest;
import com.mysawit.pengiriman.dto.ShipmentResponse;
import com.mysawit.pengiriman.enums.ShipmentStatus;
import com.mysawit.pengiriman.proto.GetShipmentByIdRequest;
import com.mysawit.pengiriman.proto.ListShipmentsByDriverRequest;
import com.mysawit.pengiriman.proto.ShipmentListMessage;
import com.mysawit.pengiriman.proto.ShipmentMessage;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipmentGrpcServiceTest {

    @Mock
    private ShipmentQueryUseCase queryUseCase;
    @Mock
    private ShipmentCommandUseCase commandUseCase;

    private ShipmentGrpcService grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new ShipmentGrpcService(
            queryUseCase, commandUseCase, new ShipmentGrpcMapper()
        );
    }

    @Test
    @DisplayName("getShipmentById should map and return gRPC message")
    void getById() {
        UUID id = UUID.randomUUID();
        when(queryUseCase.getShipmentById(id))
            .thenReturn(buildResponse(id, ShipmentStatus.MEMUAT));
        RecordingObserver<ShipmentMessage> observer = new RecordingObserver<>();

        grpcService.getShipmentById(
            GetShipmentByIdRequest.newBuilder()
                .setShipmentId(id.toString()).build(),
            observer
        );

        assertEquals(id.toString(), observer.value.getId());
        assertEquals(ShipmentStatusGrpc.MEMUAT, observer.value.getStatus());
    }

    @Test
    @DisplayName("listShipmentsByDriver should return list message")
    void listByDriver() {
        UUID id = UUID.randomUUID();
        when(queryUseCase.getShipments(any(ShipmentQueryRequest.class)))
            .thenReturn(List.of(buildResponse(id, ShipmentStatus.MENGIRIM)));
        RecordingObserver<ShipmentListMessage> observer = new RecordingObserver<>();

        grpcService.listShipmentsByDriver(
            ListShipmentsByDriverRequest.newBuilder()
                .setDriverId("d1").setDate("").build(),
            observer
        );

        assertEquals(1, observer.value.getShipmentsCount());
    }

    @Test
    @DisplayName("listShipmentsByDriver should parse date when provided")
    void listByDriverWithDate() {
        when(queryUseCase.getShipments(any(ShipmentQueryRequest.class)))
            .thenReturn(List.of());
        RecordingObserver<ShipmentListMessage> observer = new RecordingObserver<>();

        grpcService.listShipmentsByDriver(
            ListShipmentsByDriverRequest.newBuilder()
                .setDriverId("d1").setDate("2026-01-15").build(),
            observer
        );

        assertEquals(0, observer.value.getShipmentsCount());
    }

    @Test
    @DisplayName("updateDriverStatus should map gRPC enum to domain")
    void updateStatus() {
        UUID id = UUID.randomUUID();
        when(commandUseCase.updateDriverStatus(any(), any()))
            .thenReturn(buildResponse(id, ShipmentStatus.MENGIRIM));
        RecordingObserver<ShipmentMessage> observer = new RecordingObserver<>();

        grpcService.updateDriverStatus(
            UpdateDriverStatusGrpcRequest.newBuilder()
                .setShipmentId(id.toString())
                .setDriverId("d1")
                .setNewStatus(ShipmentStatusGrpc.MENGIRIM)
                .build(),
            observer
        );

        ArgumentCaptor<DriverStatusUpdateRequest> captor =
            ArgumentCaptor.forClass(DriverStatusUpdateRequest.class);
        verify(commandUseCase).updateDriverStatus(
            org.mockito.ArgumentMatchers.eq(id), captor.capture()
        );
        assertEquals(ShipmentStatus.MENGIRIM, captor.getValue().newStatus());
        assertEquals(ShipmentStatusGrpc.MENGIRIM, observer.value.getStatus());
    }

    private ShipmentResponse buildResponse(UUID id, ShipmentStatus status) {
        return new ShipmentResponse(
            id, "d1", "m1", List.of("h1"),
            new BigDecimal("150"), null, status, null,
            Instant.now(), Instant.now(), null, null
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
