package org.nevmock.digivise.infrastructure.adapter.jpa;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.domain.model.KPI;
import org.nevmock.digivise.domain.port.out.KPIRepository;
import org.nevmock.digivise.infrastructure.jpa.KPIJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaKPIRepositoryAdapter implements KPIRepository {

    private final KPIJpaRepository jpaRepository;

    @Override
    public void save(KPI kpi) {
        jpaRepository.save(kpi);
    }

    @Override
    public Optional<KPI> findKPIById(UUID kpiId) {
        return jpaRepository.findById(kpiId);
    }

    @Override
    public void deleteByID(UUID kpiId) {
        jpaRepository.deleteById(kpiId);
    }

    @Override
    public List<KPI> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Optional<KPI> findByMerchantId(UUID merchantId) {
        return jpaRepository.findByMerchant_Id(merchantId);
    }

    @Override
    public List<KPI> findByUserId(UUID userId) {
        return jpaRepository.findbyUser_Id(userId);
    }
}