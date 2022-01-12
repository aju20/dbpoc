package com.safire.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatasourceProvider {

    @Bean @LiquibaseDataSource
    public DataSource getDataSource() {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:sqlserver://test-master.database.windows.net:1433;database=MasterDB;user=testuser@test-master;password=L!L!1234;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;");
        hc.setMaximumPoolSize(1);
        return new HikariDataSource(hc);
    }
}
