package com.pmrodrigues.commons.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pmrodrigues.commons.cache.TwoLevelCacheManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.util.stream.Collectors;

/**
 * Spring cache configuration that wires up the two-level Caffeine/Redis {@link CacheManager}.
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(AppCacheProperties.class)
public class CacheConfig {

    /**
     * Creates the primary {@link CacheManager} backed by Caffeine (L1) and Redis (L2).
     *
     * @return a {@link TwoLevelCacheManager} configured from {@code AppCacheProperties}
     */
    @Bean
    @Primary
    public CacheManager cacheManager(AppCacheProperties properties,
                                     RedisConnectionFactory connectionFactory) {
        return new TwoLevelCacheManager(buildL2Manager(properties, connectionFactory), properties);
    }

    private RedisCacheManager buildL2Manager(AppCacheProperties properties,
                                             RedisConnectionFactory connectionFactory) {
        var mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var valuePair = SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(mapper));

        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(valuePair)
                .entryTtl(properties.getL2DefaultTtl());

        var perCacheConfigs = properties.getCaches().keySet().stream()
                .collect(Collectors.toMap(
                        name -> name,
                        name -> defaultConfig.entryTtl(properties.l2Ttl(name))
                ));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfigs)
                .build();
    }
}