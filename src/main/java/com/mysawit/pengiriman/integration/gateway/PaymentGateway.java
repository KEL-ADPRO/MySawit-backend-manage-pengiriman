package com.mysawit.pengiriman.integration.gateway;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {

    void triggerDriverPayroll(String driverId, UUID shipmentId, BigDecimal weightKg);

    void triggerMandorPayroll(String mandorId, UUID shipmentId, BigDecimal recognizedWeightKg);
}
