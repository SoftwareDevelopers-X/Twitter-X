package com.twitter.social.service.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    // NO CHANGE to this bean
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    // ONLY THIS BEAN CHANGES — added activateDefaultTyping
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // This one line is the actual fix.
        // It makes Jackson embed the real class name inside the JSON when
        // writing to Redis — so when reading back, it knows to reconstruct
        // PagedResponse<ReplyDto> instead of just a raw LinkedHashMap.
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(mapper);

        return builder -> builder
                .withCacheConfiguration("profiles",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(10))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                        .fromSerializer(serializer)))
                .withCacheConfiguration("profileTabs",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(2))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                        .fromSerializer(serializer)));
    }
}












//package com.twitter.social.service.config;
//
//import com.fasterxml.jackson.annotation.JsonTypeInfo;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.cache.RedisCacheConfiguration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.RedisSerializationContext;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//import java.time.Duration;
//
//@Configuration
//@EnableCaching
//public class RedisConfig {
//
//    // NO CHANGE to this bean
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(factory);
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setHashKeySerializer(new StringRedisSerializer());
//        template.setHashValueSerializer(new StringRedisSerializer());
//        template.setValueSerializer(new StringRedisSerializer());
//        template.afterPropertiesSet();
//        return template;
//    }
//
//    // ONLY THIS BEAN CHANGES — added activateDefaultTyping
//    @Bean
//    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new JavaTimeModule());
//
//        // This one line is the actual fix.
//        // It makes Jackson embed the real class name inside the JSON when
//        // writing to Redis — so when reading back, it knows to reconstruct
//        // PagedResponse<ReplyDto> instead of just a raw LinkedHashMap.
//        mapper.activateDefaultTyping(
//                LaissezFaireSubTypeValidator.instance,
//                ObjectMapper.DefaultTyping.NON_FINAL,
//                JsonTypeInfo.As.PROPERTY
//        );
//
//        GenericJackson2JsonRedisSerializer serializer =
//                new GenericJackson2JsonRedisSerializer(mapper);
//
//        return builder -> builder
//                .withCacheConfiguration("profiles",
//                        RedisCacheConfiguration.defaultCacheConfig()
//                                .entryTtl(Duration.ofMinutes(10))
//                                .serializeValuesWith(RedisSerializationContext.SerializationPair
//                                        .fromSerializer(serializer)))
//                .withCacheConfiguration("profileTabs",
//                        RedisCacheConfiguration.defaultCacheConfig()
//                                .entryTtl(Duration.ofMinutes(2))
//                                .serializeValuesWith(RedisSerializationContext.SerializationPair
//                                        .fromSerializer(serializer)));
//    }
//}