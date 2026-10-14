
package com.anas.api_gateaway.controller;

import com.anas.api_gateaway.metrics.RequestMetrics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MetricsController {

    private final RequestMetrics requestMetrics;

    public MetricsController(RequestMetrics requestMetrics) {
        this.requestMetrics = requestMetrics;
    }

    @GetMapping("/metrics")
    public Map<String, Long> getMetrics() {
        return Map.of(
                "totalRequests", requestMetrics.getTotalRequests(),
                "successfulRequests", requestMetrics.getSuccessfulRequests(),
                "failedRequests", requestMetrics.getFailedRequests(),
                "totalRetries", requestMetrics.getTotalRetries()
        );
    }
}
