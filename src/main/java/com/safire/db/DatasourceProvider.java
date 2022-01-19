package com.safire.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
public class DatasourceProvider {

    @Bean @LiquibaseDataSource
    @Profile({"azsql"})
    public DataSource getDataSource() {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:sqlserver://saftestdb.database.windows.net:1433;database=TEST;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;useBulkCopyForBatchInsert=true;");
        hc.setUsername("username");
        hc.setPassword("password");
        hc.setMaximumPoolSize(1);
        return new HikariDataSource(hc);
    }

    @Bean @LiquibaseDataSource
    @Profile({"snowflake"})
    public DataSource getSnowflakeDatasource() {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:snowflake://ci02742.north-europe.azure.snowflakecomputing.com/?warehouse=COMPUTE_WH&db=TEST&schema=SAFPOC");
        hc.setUsername("username");
        hc.setPassword("password");
        hc.setMaximumPoolSize(1);
        return new HikariDataSource(hc);
    }
}
