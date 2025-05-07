package org.nevmock.digivise.domain.port.out;

import org.nevmock.digivise.domain.model.mongo.keyword.ProductKeyword;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductKeywordRepository extends MongoRepository<ProductKeyword, String> {
     List<ProductKeyword> findByShopId(String shopId);

     List<ProductKeyword> findByCreatedAtBetween(String from, String to);

     List<ProductKeyword> findByCreatedAtGreaterThanEqual(String from);

     List<ProductKeyword> findByCreatedAtLessThanEqual(String to);

     List<ProductKeyword> findByFromAndTo(String from, String to);
 }

