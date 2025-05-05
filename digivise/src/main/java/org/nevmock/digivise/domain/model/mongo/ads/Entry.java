package org.nevmock.digivise.domain.model.mongo.ads;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
public class Entry {
    @Field("report")
    private Report report;
    @Field("title")
    private String title;
    @Field("campaign")
    private Campaign campaign;
    @Field("image")
    private String image;
    @Field("state")
    private String state;
}
