
package com.anas.api_gateaway.metrics;

import java.util.concurrent.atomic.AtomicLong;

public class RequestMetrics {

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalRetries = new AtomicLong(0);

    public void recordRequest(int statusCode) {
        totalRequests.incrementAndGet();

        if (statusCode >= 400) {
            failedRequests.incrementAndGet();
        } else {
            successfulRequests.incrementAndGet();
        }
    }

    public void recordRetry() {
        totalRetries.incrementAndGet();
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getSuccessfulRequests() {
        return successfulRequests.get();
    }

    public long getFailedRequests() {
        return failedRequests.get();
    }

    public long getTotalRetries() {
        return totalRetries.get();
    }
}
