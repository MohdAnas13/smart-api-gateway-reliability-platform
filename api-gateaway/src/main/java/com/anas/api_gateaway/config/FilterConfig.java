
package com.anas.api_gateaway.config;

import com.anas.api_gateaway.circuitbreaker.CircuitBreakerFilter;
import com.anas.api_gateaway.filter.LoggingFilter;
import com.anas.api_gateaway.history.RequestHistoryService;
import com.anas.api_gateaway.metrics.RequestMetrics;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public RequestMetrics requestMetrics() {
        return new RequestMetrics();
    }

    @Bean
    public FilterRegistrationBean<LoggingFilter> loggingFilter(
            RequestMetrics requestMetrics,
            RequestHistoryService requestHistoryService
    ) {
        FilterRegistrationBean<LoggingFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(
                new LoggingFilter(requestMetrics, requestHistoryService)
        );
        registration.addUrlPatterns("/*");
        registration.setOrder(1);

        return registration;
    }

    @Bean
    public FilterRegistrationBean<CircuitBreakerFilter> circuitBreakerFilter(
            CircuitBreakerRegistry registry
    ) {
        FilterRegistrationBean<CircuitBreakerFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(new CircuitBreakerFilter(registry));
        registration.addUrlPatterns("/users", "/users/*");
        registration.setOrder(2);

        return registration;
    }
}
