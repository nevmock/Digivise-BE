package org.nevmock.digivise.domain.model.mongo.keyword;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

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

    @Field("data")
    private DataWrapper data;
}
