package org.nevmock.digivise.config;

import org.nevmock.digivise.interfaces.Database;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class PostgresConfig implements Database {
    private PGSimpleDataSource dataSource;

    @Value("${postgres.url}")
    private String url;

    @Value("${postgres.username}")
    private String username;

    @Value("${postgres.password}")
    private String password;

    @Override
    public void connect() {
        dataSource = new PGSimpleDataSource();
        dataSource.setUrl(url);
        dataSource.setUser(username);
        dataSource.setPassword(password);
    }

    @Override
    public void disconnect() {
        dataSource = null;
    }

    @Bean
    public DataSource dataSource() {
        connect();
        return this.dataSource;
    }
}