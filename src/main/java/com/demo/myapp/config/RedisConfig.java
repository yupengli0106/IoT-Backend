package com.demo.myapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @Author: Yupeng Li
 * @Date: 2/7/2024 01:32
 * @Description: custom Redis configuration in order to use Jackson 2 Json Redis Serializer for object serialization
 */
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key serializer
        template.setKeySerializer(new StringRedisSerializer());

        // Use Jackson 2 Json Redis Serializer for value
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }

//    @PostConstruct
//    public void testRedisConnection(RedisConnectionFactory connectionFactory) {
//        RedisTemplate<String, Object> template = redisTemplate(connectionFactory);
//        template.opsForValue().set("test", "test");
//        String testValue = (String) template.opsForValue().get("test");
//        System.out.println("Redis connection test value: " + testValue);
//    }
}
