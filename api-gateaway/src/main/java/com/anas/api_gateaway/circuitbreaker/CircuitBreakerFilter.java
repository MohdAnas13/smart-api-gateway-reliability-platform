package com.anas.api_gateaway.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class CircuitBreakerFilter implements Filter {

    private final CircuitBreakerRegistry registry;

    public CircuitBreakerFilter(CircuitBreakerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (!req.getRequestURI().equals("/users")) {
            chain.doFilter(request, response);
            return;
        }

        CircuitBreaker breaker = registry.circuitBreaker("usersCircuit");

        if (breaker.getState() == CircuitBreaker.State.OPEN) {
            sendFallback(res, "Circuit breaker is OPEN");
            return;
        }

        try {
            breaker.executeRunnable(() -> {
                try {
                    chain.doFilter(request, response);
                } catch (IOException | ServletException ex) {
                    throw new RequestExecutionException(ex);
                }
            });

            if (res.getStatus() >= 500 && !res.isCommitted()) {
                sendFallback(res, "User service returned an error");
            }
        } catch (CallNotPermittedException ex) {
            sendFallback(res, "Circuit breaker is OPEN");
        } catch (RequestExecutionException ex) {
            if (!res.isCommitted()) {
                sendFallback(res, "User service request failed");
            } else {
                throw new ServletException("Request failed after response was committed", ex.getCause());
            }
        } catch (RuntimeException ex) {
            if (!res.isCommitted()) {
                sendFallback(res, "User service is unavailable");
            } else {
                throw ex;
            }
        }
    }

    private void sendFallback(HttpServletResponse response, String reason)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.resetBuffer();
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", "10");
        response.getWriter().write(
                "{\"error\":\"User service is temporarily unavailable\","
                + "\"reason\":\"" + reason + "\","
                + "\"status\":503}"
        );
    }

    private static class RequestExecutionException extends RuntimeException {
        RequestExecutionException(Throwable cause) {
            super(cause);
        }
    }
}
