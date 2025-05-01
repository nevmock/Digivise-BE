package org.nevmock.digivise.domain.model.mongo.ads;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
public class DataWrapper {
    @Field("code")
    private Integer code;
    @Field("msg")
    private String msg;
    @Field("debug_detail")
    private String debugDetail;
    @Field("data")
    private EntryList data;
}
