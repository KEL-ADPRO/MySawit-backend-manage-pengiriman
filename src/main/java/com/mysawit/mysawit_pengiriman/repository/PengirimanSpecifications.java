package com.mysawit.mysawit_pengiriman.repository;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.service.criteria.PengirimanSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class PengirimanSpecifications {

    private PengirimanSpecifications() {
    }

    public static Specification<Pengiriman> withCriteria(PengirimanSearchCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getSupirId() != null) {
                predicates.add(builder.equal(root.get("supirId"), criteria.getSupirId()));
            }
            if (criteria.getMandorId() != null) {
                predicates.add(builder.equal(root.get("mandorId"), criteria.getMandorId()));
            }
            if (criteria.getSupirNama() != null && !criteria.getSupirNama().isBlank()) {
                predicates.add(builder.like(
                    builder.lower(root.get("supirNama")),
                    "%" + criteria.getSupirNama().trim().toLowerCase() + "%"
                ));
            }
            if (criteria.getMandorNama() != null && !criteria.getMandorNama().isBlank()) {
                predicates.add(builder.like(
                    builder.lower(root.get("mandorNama")),
                    "%" + criteria.getMandorNama().trim().toLowerCase() + "%"
                ));
            }
            if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(criteria.getStatuses()));
            }
            if (criteria.getStartDateTime() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("tanggalDibuat"), criteria.getStartDateTime()));
            }
            if (criteria.getEndDateTime() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("tanggalDibuat"), criteria.getEndDateTime()));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
