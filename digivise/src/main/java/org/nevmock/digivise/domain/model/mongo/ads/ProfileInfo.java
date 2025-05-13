package org.nevmock.digivise.domain.model.mongo.ads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileInfo {
    @Field("code")
    private Integer code;
    @Field("msg")
    private String msg;
    @Field("debug_detail")
    private String debugDetail;
    @Field("data")
    private EntryList data;
}
