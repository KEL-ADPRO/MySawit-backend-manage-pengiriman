package com.mysawit.pengiriman.integration.gateway;

import com.mysawit.pengiriman.integration.dto.HarvestSummary;
import java.util.List;

public interface HarvestGateway {

    List<HarvestSummary> getApprovedHarvests(List<String> harvestIds);
}
