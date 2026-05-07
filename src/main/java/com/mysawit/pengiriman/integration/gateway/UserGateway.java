package com.mysawit.pengiriman.integration.gateway;

import com.mysawit.pengiriman.integration.dto.DriverSummary;
import java.util.List;

public interface UserGateway {

    List<DriverSummary> getDriversForMandor(String mandorId, String search);

    boolean areMandorAndDriverInSameEstate(String mandorId, String driverId);

    boolean isAdmin(String adminId);
}
