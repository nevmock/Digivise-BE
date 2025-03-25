package org.nevmock.digivise.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.nevmock.digivise.interfaces.Database;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig implements Database {
    private MongoClient mongoClient;

    @Value("${mongodb.uri}")
    private String mongoUri;

    @Value("${mongodb.database}")
    private String databaseName;

    @Override
    public void connect() {
        this.mongoClient = MongoClients.create(mongoUri);
    }

    @Override
    public void disconnect() {
        if (this.mongoClient != null) {
            this.mongoClient.close();
        }
    }

    @Bean
    public MongoDatabase mongoDatabase() {
        connect();
        return mongoClient.getDatabase(databaseName);
    }
}