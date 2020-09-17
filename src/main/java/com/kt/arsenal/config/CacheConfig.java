/*
 * Arsenal-Platform version 1.0
 * Copyright ⓒ 2019 kt corp. All rights reserved.
 * This is a proprietary software of kt corp, and you may not use this file except in
 * compliance with license agreement with kt corp. Any redistribution or use of this
 * software, with or without modification shall be strictly prohibited without prior written
 * approval of kt corp, and the copyright notice above does not evidence any actual or
 * intended publication of such software.
 */
package com.kt.arsenal.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Caffeine Cache Config
 * @author 91218672
 * @since 2019.01.17.
 * @version 1.0.0
 */
@EnableCaching
@Configuration
public class CacheConfig {

    /** Cache Refresh Time(MINUTES) **/
    public static final int CACHE_REFRESH_CYCLE = 10;

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        List<CaffeineCache> caches = Arrays.stream(CacheType.values())
                .map(cache -> new CaffeineCache(cache.getCacheName(),
                        Caffeine.newBuilder().recordStats()
                                .expireAfterWrite(cache.getExpiredAfterWrite(), TimeUnit.MINUTES)
                                .maximumSize(cache.getMaximumSize()).build()))
                .collect(Collectors.toList());
        cacheManager.setCaches(caches);
        return cacheManager;
    }


    @Getter
    public enum CacheType {
        VALIDATE_REQUEST_TOKEN("VALIDATE_REQUEST_TOKEN", 1, 100),
        BUILDER_TOPOLOGY("BUILDER_TOPOLOGY", 1, 100),
        INIT_BUSINESS_TOPOLOGY_DOMAIN("INIT_BUSINESS_TOPOLOGY_DOMAIN", 10, 100);

        private String cacheName;
        private int expiredAfterWrite;
        private int maximumSize;
        
        CacheType(String cacheName, int expiredAfterWrite, int maximumSize) {
            this.cacheName = cacheName;
            this.expiredAfterWrite = expiredAfterWrite;
            this.maximumSize = maximumSize;
        }

    }

}
