package com.tubeten.ten.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
class CacheConfig {
    
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    CacheManager redisCacheManager(RedisConnectionFactory cf) {
        var ser = new GenericJackson2JsonRedisSerializer();
        var base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(ser))
                .computePrefixWith(name -> "prod:tubeten:" + name + ":")
                .entryTtl(Duration.ofMinutes(10));

        var topCfg = base.entryTtl(Duration.ofMinutes(30)); // 30분 캐시로 더 자주 갱신하여 성능 향상

        return RedisCacheManager.builder(cf)
                .cacheDefaults(base)
                .withCacheConfiguration("top", topCfg)
                .build();
    }
    
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple")
    CacheManager simpleCacheManager() {
        return new ConcurrentMapCacheManager("top");
    }

}