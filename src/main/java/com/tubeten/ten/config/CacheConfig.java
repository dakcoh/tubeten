package com.tubeten.ten.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
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
    CacheManager cacheManager(RedisConnectionFactory cf, ObjectMapper om) {
        var ser = new GenericJackson2JsonRedisSerializer(om);
        var base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(ser))
                .computePrefixWith(name -> "prod:tubeten:" + name + ":")
                .entryTtl(Duration.ofMinutes(10));

        var topCfg = base.entryTtl(Duration.ofMinutes(30));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(base)
                .withCacheConfiguration("top", topCfg)
                .build();
    }

}