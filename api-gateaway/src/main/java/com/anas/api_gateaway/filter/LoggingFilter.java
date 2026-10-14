
package com.anas.api_gateaway.filter;

import com.anas.api_gateaway.history.RequestHistoryService;
import com.anas.api_gateaway.metrics.RequestMetrics;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class LoggingFilter implements Filter {

    private final RequestMetrics requestMetrics;
    private final RequestHistoryService requestHistoryService;

    public LoggingFilter(
            RequestMetrics requestMetrics,
            RequestHistoryService requestHistoryService
    ) {
        this.requestMetrics = requestMetrics;
        this.requestHistoryService = requestHistoryService;
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest httpRequest =
                (HttpServletRequest) request;

        HttpServletResponse httpResponse =
                (HttpServletResponse) response;

        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
        } finally {
            long duration =
                    System.currentTimeMillis() - startTime;

            int statusCode = httpResponse.getStatus();
            String method = httpRequest.getMethod();
            String path = httpRequest.getRequestURI();

            requestMetrics.recordRequest(statusCode);

            requestHistoryService.recordRequest(
                    method,
                    path,
                    statusCode,
                    duration
            );

            System.out.println("\n========== API REQUEST ==========");
            System.out.println("Method: " + method);
            System.out.println("Path: " + path);
            System.out.println("Status: " + statusCode);
            System.out.println("Duration: " + duration + " ms");
            System.out.println("=================================\n");
        }
    }
}