package org.nevmock.digivise.domain.model.mongo.ads;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
public class Campaign {
    @Field("campaign_id")
    private Long campaignId;

    @Field("daily_budget")
    private Double dailyBudget;

    @Field("end_time")
    private Integer endTime;

    @Field("total_budget")
    private Double totalBudget;
}
