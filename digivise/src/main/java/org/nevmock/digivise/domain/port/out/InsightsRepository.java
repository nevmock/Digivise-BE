package org.nevmock.digivise.domain.port.out;

import org.nevmock.digivise.domain.model.mongo.insight.Insights;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InsightsRepository extends MongoRepository<Insights, String> {
    List<Insights> findByInsight(String insight);

}
