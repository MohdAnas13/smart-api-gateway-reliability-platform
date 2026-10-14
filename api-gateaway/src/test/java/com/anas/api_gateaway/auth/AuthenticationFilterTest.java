
package com.anas.api_gateaway.auth;

import java.io.PrintWriter;
import java.io.StringWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

class AuthenticationFilterTest {

    private AuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new AuthenticationFilter();

        ReflectionTestUtils.setField(
                filter, "apiKey", "anas-secret-key"
        );

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);

        // Configure the mocked response writer
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        try {
            when(response.getWriter()).thenReturn(printWriter);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void rejectsMissingApiKey() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/users");
        when(request.getHeader("X-API-Key")).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void rejectsInvalidApiKey() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/users");
        when(request.getHeader("X-API-Key")).thenReturn("wrong-key");

        filter.doFilter(request, response, chain);

        verify(response).setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void acceptsValidApiKey() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/users");
        when(request.getHeader("X-API-Key"))
                .thenReturn("anas-secret-key");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void allowsOptionsRequestWithoutApiKey() throws Exception {
        when(request.getMethod()).thenReturn("OPTIONS");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void allowsFallbackWithoutApiKey() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI())
                .thenReturn("/fallback/users");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
