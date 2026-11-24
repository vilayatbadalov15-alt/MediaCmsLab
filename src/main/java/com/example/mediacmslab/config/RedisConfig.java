package com.example.mediacmslab.config;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class RedisConfig {
    @Bean
    RedisCacheManagerBuilderCustomizer afterCommitEvictions() {
        return builder -> builder.transactionAware();
    }
}
