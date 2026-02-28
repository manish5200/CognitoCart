package com.manish.smartcart.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Custom ObjectMapper for Redis serialization.
     * - Registers JavaTimeModule so LocalDateTime serializes correctly.
     * - Enables type info so deserialization knows the concrete class.
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Store type info in JSON so Redis can deserialize back to the correct class
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    /**
     * Default cache configuration:
     * - JSON serialization (human-readable, debuggable in Upstash console)
     * - String keys
     * - No null values stored (saves memory)
     */
    private RedisCacheConfiguration defaultCacheConfig(ObjectMapper mapper) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
    }

    /**
     * CacheManager with per-cache TTL:
     * - "products" → 10 min (changes on create/update/delete)
     * - "product-slug" → 10 min (changes on update/delete)
     * - "categories" → 60 min (rarely changes)
     * - "product-recommendations" → 60 min (for future AI feature)
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = redisObjectMapper();
        RedisCacheConfiguration defaultConfig = defaultCacheConfig(mapper).entryTtl(Duration.ofMinutes(10));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("products", defaultCacheConfig(mapper).entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("product-slug", defaultCacheConfig(mapper).entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("categories", defaultCacheConfig(mapper).entryTtl(Duration.ofMinutes(60)));
        cacheConfigs.put("product-recommendations", defaultCacheConfig(mapper).entryTtl(Duration.ofMinutes(60)));

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();

        // Wrap with LoggingCacheManager — logs every HIT, MISS, PUT, EVICT
        return new LoggingCacheManager(redisCacheManager);
    }
}
