package org.nevmock.digivise.infrastructure.adapter.jpa;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.domain.model.Merchant;
import org.nevmock.digivise.domain.port.out.MerchantRepository;
import org.nevmock.digivise.infrastructure.jpa.MerchantJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaMerchantRepositoryAdapter implements MerchantRepository {

    private final MerchantJpaRepository jpaRepository;

    @Override
    public void save(Merchant merchant) {
        jpaRepository.save(merchant);
    }

    @Override
    public Optional<Merchant> findById(UUID merchantId) {
        return jpaRepository.findById(merchantId);
    }

    @Override
    public List<Merchant> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Merchant> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<Merchant> findByShopeeMerchantId(String shopeeMerchantId) {
        return jpaRepository.getMerchantsByMerchantShopeeId(shopeeMerchantId);
    }

    @Override
    public void deleteById(UUID merchantId) {
        jpaRepository.deleteById(merchantId);
    }
}