package com.chny.dynamic.datasource.config;

import com.chny.dynamic.datasource.common.BeanRegisterFactory;
import com.chny.dynamic.datasource.common.SpringContextUtil;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "hikari")
@DependsOn(value = "SpringContextUtil")
public class DataSourcesConfig {

    private Map<String, Map<String, String>> datasources;

    @Bean
    public String initJdbcTemplate() {
        ConfigurableApplicationContext context = (ConfigurableApplicationContext) SpringContextUtil.getContext();
        datasources.entrySet().forEach(item -> {
            String name = item.getKey();
            Map<String, String> dbProperties = item.getValue();
            DataSourceProperties properties = new DataSourceProperties();
            properties.setUrl(dbProperties.get("jdbc-url"));
            properties.setUsername(dbProperties.get("username"));
            properties.setPassword(dbProperties.get("password"));
            properties.setDriverClassName(dbProperties.get("driver-class-name"));

            HikariDataSource hikariDataSource = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
            hikariDataSource.setConnectionTimeout(60000);
            hikariDataSource.setMaximumPoolSize(10);
            hikariDataSource.setMinimumIdle(5);
            hikariDataSource.setIdleTimeout(3 * 60 * 1000L);
            DataSource dataSource = hikariDataSource;
            try {
                dataSource.getConnection();
            } catch (SQLException e) {
                throw new RuntimeException("database " + name + " connect failure: " + e.getMessage());
            }
            BeanRegisterFactory.registerBean(context, name, JdbcTemplate.class, dataSource);
        });
        return datasources.toString();
    }

    public Map<String, Map<String, String>> getDatasources() {
        return datasources;
    }

    public void setDatasources(Map<String, Map<String, String>> datasources) {
        this.datasources = datasources;
    }
}
