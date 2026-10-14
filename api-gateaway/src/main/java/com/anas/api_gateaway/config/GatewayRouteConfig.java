
package com.anas.api_gateaway.config;

import com.anas.api_gateaway.filter.RetryCountFilter;
import com.anas.api_gateaway.metrics.RequestMetrics;
import org.springframework.cloud.gateway.server.mvc.filter.RetryFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.Set;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;

@Configuration
public class GatewayRouteConfig {

    @Value("${gateway.user-service-url:http://localhost:8081}")
    private String userServiceUrl;

    @Bean
    public RouterFunction<ServerResponse> userServiceRoute(
            RequestMetrics metrics) {

        return GatewayRouterFunctions.route("user-service")
                .GET("/users", HandlerFunctions.http())
                .GET("/users/**", HandlerFunctions.http())
                .before(uri(userServiceUrl))
                .filter(RetryFilterFunctions.retry(config -> config
                        .setRetries(2)
                        .setSeries(Set.of(HttpStatus.Series.SERVER_ERROR))
                        .setMethods(Set.of(HttpMethod.GET))
                        .setBackoff(
                                Duration.ofMillis(100),
                                Duration.ofMillis(500),
                                2)))
                .filter(RetryCountFilter.countRetries(metrics))
                .build();
    }
}
