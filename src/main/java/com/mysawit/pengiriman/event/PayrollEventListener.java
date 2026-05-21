package com.mysawit.pengiriman.event;

import com.mysawit.pengiriman.integration.gateway.PaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Observer Pattern: listens for shipment domain events and triggers
 * async payroll calls via the PaymentGateway.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollEventListener {

    private final PaymentGateway paymentGateway;

    @Async("payrollExecutor")
    @EventListener
    public void onMandorApproved(ShipmentApprovedByMandorEvent event) {
        log.info("Mandor approved shipment {} — triggering driver payroll for {}",
            event.shipmentId(), event.driverId());
        paymentGateway.triggerDriverPayroll(
            event.driverId(), event.shipmentId(), event.totalWeightKg()
        );
    }

    @Async("payrollExecutor")
    @EventListener
    public void onAdminApproved(ShipmentApprovedByAdminEvent event) {
        log.info("Admin approved shipment {} — triggering mandor payroll for {}",
            event.shipmentId(), event.mandorId());
        paymentGateway.triggerMandorPayroll(
            event.mandorId(), event.shipmentId(), event.recognizedWeightKg()
        );
    }

    @Async("payrollExecutor")
    @EventListener
    public void onAdminPartialRejected(ShipmentPartialRejectedByAdminEvent event) {
        log.info("Admin partially rejected shipment {} — triggering mandor payroll for recognized {}kg",
            event.shipmentId(), event.recognizedWeightKg());
        paymentGateway.triggerMandorPayroll(
            event.mandorId(), event.shipmentId(), event.recognizedWeightKg()
        );
    }
}
