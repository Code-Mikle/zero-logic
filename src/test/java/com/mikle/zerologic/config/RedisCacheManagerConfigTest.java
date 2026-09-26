package com.mikle.zerologic.config;

import com.mikle.zerologic.app.constant.AppConstant;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class RedisCacheManagerConfigTest {

    @Test
    void goodAppCacheShouldUseVersionedPrefixAndFiveMinuteTtl() {
        RedisCacheManagerConfig config = new RedisCacheManagerConfig();
        ReflectionTestUtils.setField(config, "redisConnectionFactory",
                mock(RedisConnectionFactory.class));

        RedisCacheManager cacheManager = (RedisCacheManager) config.cacheManager();
        cacheManager.afterPropertiesSet();
        RedisCacheConfiguration cacheConfiguration = cacheManager.getCacheConfigurations()
                .get(AppConstant.GOOD_APP_CACHE_NAME);

        assertEquals(Duration.ofMinutes(5),
                cacheConfiguration.getTtlFunction().getTimeToLive("key", null));
        assertEquals("zero-logic:cache:v1:good_app_page::",
                cacheConfiguration.getKeyPrefixFor(AppConstant.GOOD_APP_CACHE_NAME));
        assertFalse(cacheConfiguration.getAllowCacheNullValues());
    }
}
