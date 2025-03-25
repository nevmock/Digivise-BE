package org.nevmock.digivise.infrastructure.repository;

import org.nevmock.digivise.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    List<Merchant> findByUserId(UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE Merchant m SET m.sessionPath = ?2, m.updatedAt = CURRENT_TIMESTAMP WHERE m.id = ?1")
    int updateSessionPathByMerchantId(UUID merchantId, String sessionPath);
}