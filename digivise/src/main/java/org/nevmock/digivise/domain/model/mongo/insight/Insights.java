package org.nevmock.digivise.domain.model.mongo.insight;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "Insights")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Insights {
    @Id
    @Field("id")
    private String id;

    @Field("campaign_id")
    private Long campaignId;

    @Field("shopee_merchant_id")
    private String shopeeMerchantId;

    @Field("acos")
    private Double acos;

    @Field("cpc")
    private Double cpc;

    @Field("from")
    private String from;

    @Field("to")
    private String to;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("data_id")
    private String dataId;

    @Field("title")
    private String title;

    @Field("image")
    private String image;

    @Field("daily_budget")
    private Double dailyBudget;

    @Field("click")
    private Double click;

    @Field("ctr")
    private Double ctr;

    @Field("insight")
    private String insight;

    @Field("state")
    private String state;
}
