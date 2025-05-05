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
public class DataWrapper {
    @Field("ratio")
    private Ratio ratio;

    @Field("key")
    private String key;

    @Field("metrics")
    private Metrics metrics;
}
