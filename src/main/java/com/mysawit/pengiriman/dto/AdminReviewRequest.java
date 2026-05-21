package com.mysawit.pengiriman.dto;

import com.mysawit.pengiriman.enums.AdminReviewDecision;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdminReviewRequest(
    @NotBlank String adminId,
    @NotNull AdminReviewDecision decision,
    @DecimalMin(value = "0.0", inclusive = false) BigDecimal recognizedWeightKg,
    String rejectionReason
) {
}
