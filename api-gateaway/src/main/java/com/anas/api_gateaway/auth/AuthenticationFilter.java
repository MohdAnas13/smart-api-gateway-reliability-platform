
package com.anas.api_gateaway.auth;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthenticationFilter implements Filter {

    @Value("${gateway.api-key}")
    private String apiKey;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = httpRequest.getRequestURI();

        if (path.startsWith("/fallback/") || path.startsWith("/health") || path.startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        String providedApiKey = httpRequest.getHeader("X-API-Key");

        if (providedApiKey == null || !apiKey.equals(providedApiKey)) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("text/plain");
            httpResponse.getWriter()
                    .write("Unauthorized. Invalid or missing API key.");
            return;
        }

        chain.doFilter(request, response);
    }
}
