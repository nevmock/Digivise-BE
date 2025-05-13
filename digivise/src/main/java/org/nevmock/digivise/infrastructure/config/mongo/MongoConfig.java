package org.nevmock.digivise.infrastructure.config.mongo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MongoConfig {
    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?,?>> convs = new ArrayList<>();
        convs.add(new DocumentToProductKeywordResponseDtoConverter());
        return new MongoCustomConversions(convs);
    }
}