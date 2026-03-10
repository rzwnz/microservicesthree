package com.sthree.file.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

/**
 * Database configuration for JOOQ.
 * 
 * Configures the JOOQ DSL context for type-safe SQL query building
 * and execution against the PostgreSQL database.
 * 
 * @author rzwnz
 * @version 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class DatabaseConfig {

    private final DataSource dataSource;

    /**
     * Create the JOOQ DSL context.
     * 
     * The DSL context is the main entry point for JOOQ queries,
     * providing a fluent API for building and executing SQL.
     * 
     * @return configured DSLContext instance
     */
    @Bean
    public DSLContext dslContext() {
        // Wrap the data source to make it transaction-aware
        TransactionAwareDataSourceProxy transactionAwareDataSource = 
                new TransactionAwareDataSourceProxy(dataSource);

        // Configure JOOQ
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.setSQLDialect(SQLDialect.POSTGRES);
        configuration.setDataSource(transactionAwareDataSource);

        return DSL.using(configuration);
    }
}
