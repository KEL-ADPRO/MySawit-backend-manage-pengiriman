package com.mysawit.pengiriman.integration.client;

import com.mysawit.integration.payment.proto.PaymentGrpcServiceGrpc;
import com.mysawit.integration.payment.proto.TriggerPayrollRequest;
import com.mysawit.pengiriman.integration.gateway.PaymentGateway;
import io.grpc.StatusRuntimeException;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * gRPC client for the Payment microservice.
 * Note: async behavior is handled by PayrollEventListener, not here.
 */
@Slf4j
@Component
public class GrpcPaymentGateway implements PaymentGateway {

    @GrpcClient("payment-service")
    private PaymentGrpcServiceGrpc.PaymentGrpcServiceBlockingStub paymentStub;

    @Override
    public void triggerDriverPayroll(
        String driverId, UUID shipmentId, BigDecimal weightKg
    ) {
        triggerPayroll(driverId, shipmentId, "SUPIR", weightKg);
    }

    @Override
    public void triggerMandorPayroll(
        String mandorId, UUID shipmentId, BigDecimal recognizedWeightKg
    ) {
        triggerPayroll(mandorId, shipmentId, "MANDOR", recognizedWeightKg);
    }

    private void triggerPayroll(
        String actorId, UUID shipmentId, String role, BigDecimal weightKg
    ) {
        try {
            paymentStub.triggerPayroll(
                TriggerPayrollRequest.newBuilder()
                    .setActorId(actorId)
                    .setShipmentId(shipmentId.toString())
                    .setRole(role)
                    .setWeightKg(weightKg.toPlainString())
                    .build()
            );
        } catch (StatusRuntimeException exception) {
            log.error("Failed to trigger payroll for role={}, actor={}",
                role, actorId, exception);
            throw new IllegalStateException(
                "Failed to trigger payroll for role " + role, exception
            );
        }
    }
}
