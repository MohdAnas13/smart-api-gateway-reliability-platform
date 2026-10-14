package com.anas.api_gateaway.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CircuitBreakerController {

    private final CircuitBreakerRegistry registry;

    public CircuitBreakerController(CircuitBreakerRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/circuit-breaker/status")
    public Map<String, Object> status() {
        CircuitBreaker breaker = registry.circuitBreaker("usersCircuit");

        return Map.of(
                "name", breaker.getName(),
                "state", breaker.getState().name(),
                "failureRate", breaker.getMetrics().getFailureRate(),
                "bufferedCalls", breaker.getMetrics().getNumberOfBufferedCalls(),
                "failedCalls", breaker.getMetrics().getNumberOfFailedCalls(),
                "successfulCalls", breaker.getMetrics().getNumberOfSuccessfulCalls()
        );
    }
}
