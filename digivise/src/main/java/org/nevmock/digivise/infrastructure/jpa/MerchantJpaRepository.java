package org.nevmock.digivise.infrastructure.jpa;

import org.nevmock.digivise.domain.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;
import java.util.UUID;

@EnableJpaRepositories(basePackageClasses = MerchantJpaRepository.class)
public interface MerchantJpaRepository extends JpaRepository<Merchant, UUID> {
    List<Merchant> findByUserId(UUID userId);
}