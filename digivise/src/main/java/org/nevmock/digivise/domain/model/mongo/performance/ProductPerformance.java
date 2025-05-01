package org.nevmock.digivise.domain.model.mongo.performance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "product_performance")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductPerformance {

    @Id
    private String id;

    private int code;
    private String msg;
    private Result result;
}
