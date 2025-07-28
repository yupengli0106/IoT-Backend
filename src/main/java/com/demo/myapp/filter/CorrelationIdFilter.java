package com.demo.myapp.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * @Author: Yupeng Li
 * @Date: 2024-07-27
 * @Description: IMPROVED - Correlation ID filter for request tracing
 * 
 * This filter adds a unique correlation ID to each request for better log traceability.
 * The correlation ID is:
 * - Generated for each new request or extracted from X-Correlation-ID header
 * - Added to MDC (Mapped Diagnostic Context) for logging
 * - Returned in response headers for client-side tracing
 * - Automatically cleaned up after request completion
 */

@Component
@Order(1) // Execute before JWT filter
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Get or generate correlation ID
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        try {
            // Add to MDC for logging
            MDC.put(CORRELATION_ID_KEY, correlationId);
            
            // Add to response headers for client tracing
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            // Continue with the filter chain
            filterChain.doFilter(request, response);
        } finally {
            // IMPORTANT: Always clean up MDC to prevent memory leaks
            MDC.clear();
        }
    }
}