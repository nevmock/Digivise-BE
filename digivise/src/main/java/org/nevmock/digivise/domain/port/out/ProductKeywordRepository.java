package org.nevmock.digivise.domain.port.out;

import org.jetbrains.annotations.NotNull;
import org.nevmock.digivise.domain.model.mongo.keyword.ProductKeyword;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductKeywordRepository extends MongoRepository<ProductKeyword, String> {
     List<ProductKeyword> findByShopId(String shopId);

     List<ProductKeyword> findByCreatedAtBetween(String from, String to);

     List<ProductKeyword> findByCreatedAtGreaterThanEqual(String from);

     List<ProductKeyword> findByCreatedAtLessThanEqual(String to);

     List<ProductKeyword> findByFromAndTo(String from, String to);

     List<ProductKeyword> findByCampaignId(Long campaignId);

     List<ProductKeyword> findByShopIdAndCreatedAtBetween(
           String shopId,
           LocalDateTime from,
           LocalDateTime to
     );

     List<ProductKeyword> findByCampaignIdAndCreatedAtBetween(
           Long campaignId,
           LocalDateTime from,
           LocalDateTime to
     );

     @NotNull Optional<ProductKeyword> findById(@NotNull String id);
}

