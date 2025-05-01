package org.nevmock.digivise.domain.port.out;

import org.nevmock.digivise.domain.model.mongo.ads.ProductAds;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductAdsRepository extends MongoRepository<ProductAds, String> {
    List<ProductAds> findAll();

    Optional<ProductAds> findById(String id);

    List<ProductAds> findByShopId(String shopId);

    List<ProductAds> findByCreatedAtBetween(String from, String to);

    List<ProductAds> findByCreatedAtGreaterThanEqual(String from);

    List<ProductAds> findByCreatedAtLessThanEqual(String to);

    List<ProductAds> findByFromAndTo(String from, String to);

    Page<ProductAds> findByShopIdAndCreatedAtBetween(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
}
