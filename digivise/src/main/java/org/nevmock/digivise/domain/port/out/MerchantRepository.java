package org.nevmock.digivise.domain.port.out;

import org.nevmock.digivise.domain.model.Merchant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository {
    void save(Merchant merchant);

    Optional<Merchant> findById(UUID merchantId);

    List<Merchant> findAll();

    List<Merchant> findByUserId(UUID userId);

    Optional<Merchant> findByShopeeMerchantId(String shopeeMerchantId);

    void deleteById(UUID merchantId);
}
