package com.mysawit.pengiriman.integration.client;

import com.mysawit.integration.harvest.proto.GetApprovedHarvestsRequest;
import com.mysawit.integration.harvest.proto.HarvestGrpcServiceGrpc;
import com.mysawit.integration.harvest.proto.HarvestSummaryMessage;
import com.mysawit.pengiriman.integration.dto.HarvestSummary;
import com.mysawit.pengiriman.integration.gateway.HarvestGateway;
import io.grpc.StatusRuntimeException;
import java.math.BigDecimal;
import java.util.List;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class GrpcHarvestGateway implements HarvestGateway {

    @GrpcClient("harvest-service")
    private HarvestGrpcServiceGrpc.HarvestGrpcServiceBlockingStub harvestStub;

    @Override
    public List<HarvestSummary> getApprovedHarvests(List<String> harvestIds) {
        try {
            return harvestStub.getApprovedHarvests(
                    GetApprovedHarvestsRequest.newBuilder()
                        .addAllHarvestIds(harvestIds)
                        .build()
                )
                .getHarvestsList()
                .stream()
                .map(this::toHarvestSummary)
                .toList();
        } catch (StatusRuntimeException exception) {
            throw new IllegalStateException("Failed to fetch harvests from harvest module", exception);
        }
    }

    private HarvestSummary toHarvestSummary(HarvestSummaryMessage message) {
        return new HarvestSummary(
            message.getId(),
            new BigDecimal(message.getWeightKg()),
            message.getApproved()
        );
    }
}
