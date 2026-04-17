package com.mysawit.mysawit_pengiriman.service.criteria;

import com.mysawit.mysawit_pengiriman.model.StatusPengiriman;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class PengirimanSearchCriteria {
    private UUID supirId;
    private UUID mandorId;
    private String supirNama;
    private String mandorNama;
    private List<StatusPengiriman> statuses;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
}
