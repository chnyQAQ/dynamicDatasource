package com.chny.dynamic.datasource.config;

import com.chny.dynamic.datasource.common.SpringContextUtil;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "hikari")
public class DataSourcesConfig {

    private Map<String, Map<String, String>> datasources;

    @Bean
    public SpringContextUtil getSpringContextUtil() {
        return new SpringContextUtil();
    }

    @Order(1)
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
                throw new RuntimeException("database " + name + "connect failure: " + e.getMessage());
            }
            registerBean(context, name, JdbcTemplate.class, dataSource);
        });
        return datasources.toString();
    }

    public static <T> T registerBean(ConfigurableApplicationContext applicationContext, String beanName, Class<T> clazz, Object... args) {
        if (applicationContext.containsBean(beanName)) {
            Object bean = applicationContext.getBean(beanName);
            if (bean.getClass().isAssignableFrom(clazz)) {
                return (T) bean;
            } else {
                throw new RuntimeException("Error create bean with name '" + beanName +"'.");
            }
        }
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        for (Object arg : args) {
            beanDefinitionBuilder.addConstructorArgValue(arg);
        }
        BeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();

        BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) applicationContext.getBeanFactory();
        beanFactory.registerBeanDefinition(beanName, beanDefinition);
        return applicationContext.getBean(beanName, clazz);
    }

    public Map<String, Map<String, String>> getDatasources() {
        return datasources;
    }

    public void setDatasources(Map<String, Map<String, String>> datasources) {
        this.datasources = datasources;
    }
}
