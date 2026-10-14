
package com.anas.api_gateaway.controller;

import com.anas.api_gateaway.history.RequestHistory;
import com.anas.api_gateaway.history.RequestHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RequestHistoryController {

    private final RequestHistoryService requestHistoryService;

    public RequestHistoryController(
            RequestHistoryService requestHistoryService
    ) {
        this.requestHistoryService = requestHistoryService;
    }

    @GetMapping("/requests")
    public List<RequestHistory> getRequestHistory() {
        return requestHistoryService.getRecentRequests();
    }
}