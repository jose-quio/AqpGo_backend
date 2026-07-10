package com.integrador.Turismo.Config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Value("${app.datasource.primary.url}")
    private String primaryUrl;
    @Value("${app.datasource.primary.username}")
    private String primaryUser;
    @Value("${app.datasource.primary.password}")
    private String primaryPassword;

    @Value("${app.datasource.secondary.url}")
    private String secondaryUrl;
    @Value("${app.datasource.secondary.username}")
    private String secondaryUser;
    @Value("${app.datasource.secondary.password}")
    private String secondaryPassword;

    @Bean
    public DataSource primaryDataSource() {
        HikariDataSource ds = DataSourceBuilder.create()
                .url(primaryUrl)
                .username(primaryUser)
                .password(primaryPassword)
                .type(HikariDataSource.class)
                .build();
        ds.setPoolName("primary-supabase");
        // Timeout corto: si Supabase esta caido, que falle rapido
        // en vez de colgar la app varios segundos por conexion.
        ds.setConnectionTimeout(5000);
        ds.setMaximumPoolSize(5);
        return ds;
    }

    @Bean
    public DataSource secondaryDataSource() {
        HikariDataSource ds = DataSourceBuilder.create()
                .url(secondaryUrl)
                .username(secondaryUser)
                .password(secondaryPassword)
                .type(HikariDataSource.class)
                .build();
        ds.setPoolName("secondary-local-docker");
        ds.setConnectionTimeout(5000);
        ds.setMaximumPoolSize(5);
        return ds;
    }

    @Bean
    @Primary
    public DataSource routingDataSource(DataSource primaryDataSource, DataSource secondaryDataSource) {
        RoutingDataSource routingDataSource = new RoutingDataSource();

        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put(DataSourceContextHolder.DataSourceType.PRIMARY, primaryDataSource);
        dataSources.put(DataSourceContextHolder.DataSourceType.SECONDARY, secondaryDataSource);

        routingDataSource.setTargetDataSources(dataSources);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);
        routingDataSource.afterPropertiesSet();

        return routingDataSource;
    }
}
