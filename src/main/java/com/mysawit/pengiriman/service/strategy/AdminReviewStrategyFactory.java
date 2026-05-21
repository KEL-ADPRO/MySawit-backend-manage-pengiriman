package com.mysawit.pengiriman.service.strategy;

import com.mysawit.pengiriman.enums.AdminReviewDecision;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Factory that resolves the correct AdminReviewStrategy by decision type.
 */
@Component
public class AdminReviewStrategyFactory {

    private final Map<AdminReviewDecision, AdminReviewStrategy> strategyMap;

    public AdminReviewStrategyFactory(
        ApproveAdminStrategy approveStrategy,
        RejectAdminStrategy rejectStrategy,
        PartialRejectAdminStrategy partialRejectStrategy
    ) {
        this.strategyMap = new EnumMap<>(AdminReviewDecision.class);
        this.strategyMap.put(AdminReviewDecision.APPROVE, approveStrategy);
        this.strategyMap.put(AdminReviewDecision.REJECT, rejectStrategy);
        this.strategyMap.put(AdminReviewDecision.PARTIAL_REJECT, partialRejectStrategy);
    }

    public AdminReviewStrategy resolve(AdminReviewDecision decision) {
        AdminReviewStrategy strategy = strategyMap.get(decision);
        if (strategy == null) {
            throw new IllegalArgumentException(
                "Unsupported admin review decision: " + decision
            );
        }
        return strategy;
    }
}
