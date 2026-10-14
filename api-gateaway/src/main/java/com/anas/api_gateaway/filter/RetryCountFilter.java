
package com.anas.api_gateaway.filter;

import com.anas.api_gateaway.metrics.RequestMetrics;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.concurrent.atomic.AtomicInteger;

public class RetryCountFilter {

    private static final String ATTEMPT_KEY =
            RetryCountFilter.class.getName() + ".attempts";

    public static HandlerFilterFunction<ServerResponse, ServerResponse> countRetries(
            RequestMetrics metrics) {

        return (request, next) -> {
            AtomicInteger attempts = (AtomicInteger) request.attributes()
                    .computeIfAbsent(ATTEMPT_KEY, key -> new AtomicInteger(0));

            int attempt = attempts.incrementAndGet();

            if (attempt > 1) {
                metrics.recordRetry();
            }

            return next.handle(request);
        };
    }
}
