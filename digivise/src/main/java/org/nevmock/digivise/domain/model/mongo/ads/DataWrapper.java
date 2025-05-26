package org.nevmock.digivise.domain.model.mongo.ads;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
public class DataWrapper {
    @Field("entry_list")
    private EntryList entryList;
    @Field("total")
    private Integer total;
}
