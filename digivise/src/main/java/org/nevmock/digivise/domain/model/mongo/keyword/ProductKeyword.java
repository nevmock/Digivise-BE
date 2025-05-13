package org.nevmock.digivise.domain.model.mongo.keyword;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "ProductKey")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductKeyword {
    @Id
    @Field("_id")
    private String id;

    @Field("code")
    private Integer code;

    @Field("msg")
    private String msg;

    @Field("debug_detail")
    private String debugDetail;

    @Field("validation_error_list")
    private String validationErrorList;

    @Field("shop_id")
    private String shopId;

    @Field("data")
    private List<DataWrapper> data;

    @Field("createdAt")
    private LocalDateTime createdAt;

    @Field("from")
    private String from;

    @Field("to")
    private String to;

    @Field("updatedAt")
    private String updatedAt;

    @Field("campaign_id")
    private Long campaignId;
}
