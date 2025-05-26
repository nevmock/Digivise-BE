package org.nevmock.digivise.domain.model.mongo.ads;

import com.mongodb.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "ProductAds")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductAds {
    @Id
    @Field("_id")
    private String id;
    @Nullable
    @Field("shop_id")
    private String shopId;
    @Field("createdAt")
    private LocalDateTime createdAt;
    @Field("from")
    private String from;
    @Field("to")
    private String to;
    @Field("data")
    private DataWrapper data;
}
