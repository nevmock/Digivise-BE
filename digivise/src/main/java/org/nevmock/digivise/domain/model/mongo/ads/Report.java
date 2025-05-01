package org.nevmock.digivise.domain.model.mongo.ads;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
public class Report {
    @Field("broad_cir")
    private Double broad_cir;

    @Field("broad_gmv")
    private Double broad_gmv;

    @Field("broad_order")
    private Double broad_order;

    @Field("broad_order_amount")
    private Double broad_order_amount;

    @Field("broad_roi")
    private Double broad_roi;

    @Field("checkout")
    private Double checkout;

    @Field("checkout_rate")
    private Double checkout_rate;

    @Field("click")
    private Double click;

    @Field("cost")
    private Double cost;

    @Field("cpc")
    private Double cpc;

    @Field("cpdc")
    private Double cpdc;

    @Field("cr")
    private Double cr;

    @Field("ctr")
    private Double ctr;

    @Field("direct_cr")
    private Double direct_cr;

    @Field("direct_cir")
    private Double direct_cir;

    @Field("direct_gmv")
    private Double direct_gmv;

    @Field("direct_order")
    private Double direct_order;

    @Field("direct_order_amount")
    private Double direct_order_amount;

    @Field("direct_roi")
    private Double direct_roi;

    @Field("impression")
    private Double impression;

    @Field("avg_rank")
    private Double avg_rank;

    @Field("product_click")
    private Double product_click;

    @Field("product_impression")
    private Double product_impression;

    @Field("product_ctr")
    private Double product_ctr;

    @Field("location_in_ads")
    private Double location_in_ads;
}

