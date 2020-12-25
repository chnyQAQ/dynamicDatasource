package com.chny.dynamic.datasource.config;

import com.chny.dynamic.datasource.common.BeanRegisterFactory;
import com.chny.dynamic.datasource.common.SpringContextUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "redis")
@DependsOn(value = "SpringContextUtil")
public class RedisConfig {

    private Map<String, Map<String, String>> properties;

    @Bean
    public String initRedisTemplate() {
        ConfigurableApplicationContext context = (ConfigurableApplicationContext) SpringContextUtil.getContext();
        properties.entrySet().forEach(item -> {
            String name = item.getKey();
            Map<String, String> redisProperties = item.getValue();

            RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
            configuration.setHostName(redisProperties.get("hostname"));
            configuration.setPort(Integer.parseInt(redisProperties.get("port")));
            configuration.setPassword(RedisPassword.of(redisProperties.get("password")));
            LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(configuration);
            lettuceConnectionFactory.afterPropertiesSet();
            lettuceConnectionFactory.initConnection();

            RedisTemplate redisTemplate = BeanRegisterFactory.registerBean(context, name, RedisTemplate.class, lettuceConnectionFactory);
            redisTemplate.afterPropertiesSet();
            Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
            redisTemplate.setKeySerializer(jackson2JsonRedisSerializer);
            redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
            redisTemplate.setHashKeySerializer(jackson2JsonRedisSerializer);
            redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        });
        return properties.toString();
    }

    public Map<String, Map<String, String>> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Map<String, String>> properties) {
        this.properties = properties;
    }
}
