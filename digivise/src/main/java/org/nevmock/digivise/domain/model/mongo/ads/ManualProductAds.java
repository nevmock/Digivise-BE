package org.nevmock.digivise.domain.model.mongo.ads;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
public class ManualProductAds {
    @Field("bidding_strategy")
    private String biddingStrategy;
    @Field("cps")
    private Boolean cps;
    @Field("item_id")
    private Double itemId;
    @Field("product_placement")
    private String productPlacement;
    @Field("simple_roi_one_no_edit_timestamp")
    private String simpleRoiOneNoEditTimestamp;
    @Field("target_audience")
    private String targetAudience;
}
