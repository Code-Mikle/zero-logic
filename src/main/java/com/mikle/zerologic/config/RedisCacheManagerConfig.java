package com.mikle.zerologic.config;

import com.mikle.zerologic.app.constant.AppConstant;
import jakarta.annotation.Resource;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 缓存管理器配置
 */
@Configuration
public class RedisCacheManagerConfig {

    private static final String CACHE_KEY_PREFIX = "zero-logic:cache:v1:";

    private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(30);

    private static final Duration GOOD_APP_CACHE_TTL = Duration.ofMinutes(5);

    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    public CacheManager cacheManager() {
        // 默认配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_CACHE_TTL)
                // 方法返回 null 时不写入缓存；这不等同于防止缓存穿透
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> CACHE_KEY_PREFIX + cacheName + "::")
                // key 使用 String 序列化器
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                // 精选应用分页缓存使用更短的过期时间
                .withCacheConfiguration(AppConstant.GOOD_APP_CACHE_NAME,
                        defaultConfig.entryTtl(GOOD_APP_CACHE_TTL))
                .build();
    }
}
