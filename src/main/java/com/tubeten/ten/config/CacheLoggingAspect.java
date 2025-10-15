package com.tubeten.ten.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class CacheLoggingAspect {

    private final CacheManager cacheManager;

    public CacheLoggingAspect(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object logCacheAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // 캐시 키 생성 (간단한 버전)
        String cacheKey = generateCacheKey(methodName, args);
        
        // 캐시에서 데이터 확인
        Cache cache = cacheManager.getCache("top");
        boolean cacheHit = cache != null && cache.get(cacheKey) != null;
        
        if (cacheHit) {
            log.info("Redis 캐시 조회 성공 - method: {}", methodName);
        }
        
        Object result = joinPoint.proceed();
        
        if (!cacheHit) {
            log.info("Redis 캐시 저장 완료 - method: {}", methodName);
        }
        
        return result;
    }
    
    private String generateCacheKey(String methodName, Object[] args) {
        StringBuilder key = new StringBuilder(methodName);
        for (Object arg : args) {
            key.append(":").append(arg != null ? arg.toString() : "null");
        }
        return key.toString();
    }
}