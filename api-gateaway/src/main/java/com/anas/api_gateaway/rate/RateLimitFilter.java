
package com.anas.api_gateaway.rate;

import java.io.IOException;
import java.time.Duration;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.data.redis.core.StringRedisTemplate;

public class RateLimitFilter implements Filter {

    private static final int LIMIT = 100;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;

    public RateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Allow CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Create a separate counter for each client IP
        String clientIp = httpRequest.getRemoteAddr();
        String redisKey = "rate-limit:" + clientIp;

        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);

            if (count == null) {
                httpResponse.sendError(
                        HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "Rate limiter temporarily unavailable."
                );
                return;
            }

            // Start a one-minute expiry window for a new counter
            if (count == 1) {
                redisTemplate.expire(redisKey, WINDOW);
            }

            if (count > LIMIT) {
                httpResponse.setStatus(429);
                httpResponse.setContentType("text/plain");
                httpResponse.setHeader("Retry-After", "60");
                httpResponse.getWriter().write(
                        "Too many requests. Please try again later."
                );
                return;
            }

            chain.doFilter(request, response);

        } catch (org.springframework.data.redis.RedisConnectionFailureException e) {
            httpResponse.sendError(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Rate limiter unavailable. Please try again later."
            );
        }
    }
}


