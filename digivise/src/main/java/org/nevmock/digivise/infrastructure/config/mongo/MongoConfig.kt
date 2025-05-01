package org.nevmock.digivise.infrastructure.config.mongo

//@Configuration
//open class MongoConfig {
//
//    @Value("\${mongodb.uri}")
//    private lateinit var mongoUri: String
//
//    @Bean
//    open fun mongoClient() : MongoClient {
//        val connectionString = ConnectionString(mongoUri)
//
//        val settings = MongoClientSettings.builder()
//            .applyConnectionString(connectionString)
//            .build()
//
//        return MongoClients.create(settings)
//    }
//}