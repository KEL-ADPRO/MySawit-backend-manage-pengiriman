package com.mysawit.pengiriman.integration.client;

import com.mysawit.integration.payment.proto.PaymentGrpcServiceGrpc;
import com.mysawit.integration.payment.proto.TriggerPayrollRequest;
import com.mysawit.pengiriman.integration.gateway.PaymentGateway;
import io.grpc.StatusRuntimeException;
import java.math.BigDecimal;
import java.util.UUID;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class GrpcPaymentGateway implements PaymentGateway {

    @GrpcClient("payment-service")
    private PaymentGrpcServiceGrpc.PaymentGrpcServiceBlockingStub paymentStub;

    @Override
    @Async("payrollExecutor")
    public void triggerDriverPayroll(String driverId, UUID shipmentId, BigDecimal weightKg) {
        triggerPayroll(driverId, shipmentId, "SUPIR", weightKg);
    }

    @Override
    @Async("payrollExecutor")
    public void triggerMandorPayroll(String mandorId, UUID shipmentId, BigDecimal recognizedWeightKg) {
        triggerPayroll(mandorId, shipmentId, "MANDOR", recognizedWeightKg);
    }

    private void triggerPayroll(String actorId, UUID shipmentId, String role, BigDecimal weightKg) {
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
            throw new IllegalStateException("Failed to trigger payroll for role " + role, exception);
        }
    }
}
