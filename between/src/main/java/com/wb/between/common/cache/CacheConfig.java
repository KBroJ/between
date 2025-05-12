package com.wb.between.common.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // 별도 라이브러리 없이 스프링 기본 메모리 캐시 사용
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("headerMenus");
    }
}
