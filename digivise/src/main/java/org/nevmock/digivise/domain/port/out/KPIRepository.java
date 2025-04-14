package org.nevmock.digivise.domain.port.out;

import org.nevmock.digivise.domain.model.KPI;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KPIRepository {
    void save(KPI kpi);
    Optional<KPI> findKPIById(UUID kpiId);
    void deleteByID(UUID kpiId);
    List<KPI> findAll();
    Optional<KPI> findByMerchantId(UUID merchantId);
    List<KPI> findByUserId(UUID userId);
}
