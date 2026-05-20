package com.mysawit.pengiriman.event;

import static org.mockito.Mockito.verify;

import com.mysawit.pengiriman.integration.gateway.PaymentGateway;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayrollEventListenerTest {

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PayrollEventListener listener;

    @Test
    @DisplayName("should trigger driver payroll on mandor approval")
    void onMandorApproved() {
        UUID shipmentId = UUID.randomUUID();
        ShipmentApprovedByMandorEvent event =
            new ShipmentApprovedByMandorEvent("driver-1", shipmentId, new BigDecimal("180"));

        listener.onMandorApproved(event);

        verify(paymentGateway).triggerDriverPayroll("driver-1", shipmentId, new BigDecimal("180"));
    }

    @Test
    @DisplayName("should trigger mandor payroll on admin approval")
    void onAdminApproved() {
        UUID shipmentId = UUID.randomUUID();
        ShipmentApprovedByAdminEvent event =
            new ShipmentApprovedByAdminEvent("mandor-1", shipmentId, new BigDecimal("200"));

        listener.onAdminApproved(event);

        verify(paymentGateway).triggerMandorPayroll("mandor-1", shipmentId, new BigDecimal("200"));
    }

    @Test
    @DisplayName("should trigger mandor payroll on admin partial rejection")
    void onAdminPartialRejected() {
        UUID shipmentId = UUID.randomUUID();
        ShipmentPartialRejectedByAdminEvent event =
            new ShipmentPartialRejectedByAdminEvent("mandor-1", shipmentId, new BigDecimal("125"));

        listener.onAdminPartialRejected(event);

        verify(paymentGateway).triggerMandorPayroll("mandor-1", shipmentId, new BigDecimal("125"));
    }
}
