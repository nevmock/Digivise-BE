package org.nevmock.digivise.infrastructure.jpa;

import org.nevmock.digivise.domain.model.KPI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@EnableJpaRepositories(basePackageClasses = KPIJpaRepository.class)
public interface KPIJpaRepository extends JpaRepository<KPI, UUID> {
    List<KPI> findByUser_Id(UUID userId);
    Optional<KPI> findByMerchant_Id(UUID merchantId);
}