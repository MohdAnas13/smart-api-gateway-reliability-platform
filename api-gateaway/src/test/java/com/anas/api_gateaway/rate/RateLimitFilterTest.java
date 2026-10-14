
package com.anas.api_gateaway.rate;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private StringWriter responseBody;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws Exception {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        filter = new RateLimitFilter(redisTemplate);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);

        responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void allowsRequestWithinLimit() throws Exception {
        when(valueOperations.increment("rate-limit:127.0.0.1"))
                .thenReturn(1L);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(redisTemplate).expire(
                "rate-limit:127.0.0.1", Duration.ofMinutes(1));
    }

    @Test
    void rejectsRequestAboveLimit() throws Exception {
        when(valueOperations.increment("rate-limit:127.0.0.1"))
                .thenReturn(101L);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "60");
        assertTrue(responseBody.toString().contains("Too many requests"));
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void allowsOptionsRequestWithoutCheckingRedis() throws Exception {
        when(request.getMethod()).thenReturn("OPTIONS");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redisTemplate);
    }
}
