package org.nevmock.digivise.infrastructure.config.mongo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

@Configuration
public class MongoIndexesConfig implements ApplicationRunner {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // Indexes for ProductAds collection
        IndexOperations adsOps = mongoTemplate.indexOps("ProductAds");
        // compound index on shop_id and createdAt
        adsOps.ensureIndex(
                new Index()
                        .on("shop_id", Sort.Direction.ASC)
                        .on("createdAt", Sort.Direction.ASC)
                        .named("idx_shop_createdAt")
        );
        // index on data.entry_list.manual_product_ads.bidding_strategy
        adsOps.ensureIndex(
                new Index()
                        .on("data.entry_list.manual_product_ads.bidding_strategy", Sort.Direction.ASC)
                        .named("idx_biddingStrategy")
        );
        // index on data.entry_list.type
        adsOps.ensureIndex(
                new Index()
                        .on("data.entry_list.type", Sort.Direction.ASC)
                        .named("idx_entryType")
        );

        // Indexes for ProductKey collection
        IndexOperations keyOps = mongoTemplate.indexOps("ProductKey");
        // index on campaign_id
        keyOps.ensureIndex(
                new Index()
                        .on("campaign_id", Sort.Direction.ASC)
                        .named("idx_productKey_campaignId")
        );

        // Indexes for ProductStock collection
        IndexOperations stockOps = mongoTemplate.indexOps("ProductStock");
        // index on data.boost_info.campaign_id
        stockOps.ensureIndex(
                new Index()
                        .on("data.boost_info.campaign_id", Sort.Direction.ASC)
                        .named("idx_stock_campaignId")
        );
        // compound index on data.boost_info.campaign_id and createdAt
        stockOps.ensureIndex(
                new Index()
                        .on("data.boost_info.campaign_id", Sort.Direction.ASC)
                        .on("createdAt", Sort.Direction.ASC)
                        .named("idx_stock_campaign_date")
        );

        System.out.println("[MongoIndexesConfig] All required indexes created or already existed.");
    }
}
