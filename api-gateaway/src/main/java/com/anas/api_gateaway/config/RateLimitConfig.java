
package com.anas.api_gateaway.config;

import com.anas.api_gateaway.rate.RateLimitFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimitFilter rateLimitFilter(
            StringRedisTemplate redisTemplate
    ) {
        return new RateLimitFilter(redisTemplate);
    }
}
