package org.nevmock.digivise.domain.model.mongo.keyword;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Metrics {
    @Field("broad_cir")
    private Double broadCir;

    @Field("broad_gmv")
    private Double broadGmv;

    @Field("click")
    private Double click;

    @Field("cost")
    private Double cost;

    @Field("cpc")
    private Double cpc;

    @Field("ctr")
    private Double ctr;

    @Field("impression")
    private Double impression;
}
