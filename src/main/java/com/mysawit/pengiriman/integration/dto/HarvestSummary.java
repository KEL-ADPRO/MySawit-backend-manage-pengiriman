package com.mysawit.pengiriman.integration.dto;

import java.math.BigDecimal;

public record HarvestSummary(
    String id,
    BigDecimal weightKg,
    boolean approved
) {
}
