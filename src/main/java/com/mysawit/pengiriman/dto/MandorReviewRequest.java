package com.mysawit.pengiriman.dto;

import jakarta.validation.constraints.NotBlank;

public record MandorReviewRequest(
    @NotBlank String mandorId,
    boolean approved,
    String rejectionReason
) {
}
