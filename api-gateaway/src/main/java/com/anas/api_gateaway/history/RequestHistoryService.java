
package com.anas.api_gateaway.history;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class RequestHistoryService {

    private static final int MAX_HISTORY = 100;

    private final ConcurrentLinkedDeque<RequestHistory> history =
            new ConcurrentLinkedDeque<>();

    public void recordRequest(
            String method,
            String path,
            int statusCode,
            long durationMs
    ) {
        RequestHistory request = new RequestHistory(
                method,
                path,
                statusCode,
                durationMs,
                LocalDateTime.now()
        );

        history.addFirst(request);

        while (history.size() > MAX_HISTORY) {
            history.pollLast();
        }
    }

    public List<RequestHistory> getRecentRequests() {
        return new ArrayList<>(history);
    }
}