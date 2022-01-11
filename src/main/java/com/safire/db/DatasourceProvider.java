package com.safire.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DatasourceProvider {

    @Bean @LiquibaseDataSource
    public DataSource getDataSource() {
        return new HikariDataSource(new HikariConfig());
    }
}
