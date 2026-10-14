
package com.anas.api_gateaway.history;

import java.time.LocalDateTime;

public class RequestHistory {

    private final String method;
    private final String path;
    private final int statusCode;
    private final long durationMs;
    private final LocalDateTime timestamp;

    public RequestHistory(
            String method,
            String path,
            int statusCode,
            long durationMs,
            LocalDateTime timestamp
    ) {
        this.method = method;
        this.path = path;
        this.statusCode = statusCode;
        this.durationMs = durationMs;
        this.timestamp = timestamp;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}