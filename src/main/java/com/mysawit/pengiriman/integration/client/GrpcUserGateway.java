package com.mysawit.pengiriman.integration.client;

import com.mysawit.integration.user.proto.DriverSummaryMessage;
import com.mysawit.integration.user.proto.ListDriversByMandorRequest;
import com.mysawit.integration.user.proto.UserGrpcServiceGrpc;
import com.mysawit.integration.user.proto.ValidateAdminRoleRequest;
import com.mysawit.integration.user.proto.ValidateMandorDriverEstateRequest;
import com.mysawit.pengiriman.integration.dto.DriverSummary;
import com.mysawit.pengiriman.integration.gateway.UserGateway;
import com.mysawit.pengiriman.profiling.Profiled;
import io.grpc.StatusRuntimeException;
import java.util.List;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Profiled(category = "shipment.grpc.user")
@Component
public class GrpcUserGateway implements UserGateway {

    @GrpcClient("user-service")
    private UserGrpcServiceGrpc.UserGrpcServiceBlockingStub userStub;

    @Override
    public List<DriverSummary> getDriversForMandor(String mandorId, String search) {
        try {
            return userStub.listDriversByMandor(
                    ListDriversByMandorRequest.newBuilder()
                        .setMandorId(mandorId)
                        .setSearch(search == null ? "" : search)
                        .build()
                )
                .getDriversList()
                .stream()
                .map(this::toDriverSummary)
                .toList();
        } catch (StatusRuntimeException exception) {
            throw new IllegalStateException("Failed to fetch drivers from user module", exception);
        }
    }

    @Override
    public boolean areMandorAndDriverInSameEstate(String mandorId, String driverId) {
        try {
            return userStub.validateMandorDriverEstate(
                    ValidateMandorDriverEstateRequest.newBuilder()
                        .setMandorId(mandorId)
                        .setDriverId(driverId)
                        .build()
                )
                .getValid();
        } catch (StatusRuntimeException exception) {
            throw new IllegalStateException("Failed to validate mandor and driver assignment", exception);
        }
    }

    @Override
    public boolean isAdmin(String adminId) {
        try {
            return userStub.validateAdminRole(
                    ValidateAdminRoleRequest.newBuilder()
                        .setAdminId(adminId)
                        .build()
                )
                .getValid();
        } catch (StatusRuntimeException exception) {
            throw new IllegalStateException("Failed to validate admin role", exception);
        }
    }

    private DriverSummary toDriverSummary(DriverSummaryMessage message) {
        return new DriverSummary(message.getId(), message.getName(), message.getEstateId());
    }
}
