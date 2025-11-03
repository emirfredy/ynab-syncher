package co.personal.ynabsyncher.infrastructure.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to ensure all HTTP requests have a correlation ID for tracing.
 * Checks for existing X-Correlation-ID header, or generates a new UUID.
 * Adds correlation ID to MDC for logging and to response headers.
 */
@Component
public class CorrelationIdFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION_ID_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Get existing correlation ID from request header or generate new one
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }

            // Add to MDC for logging context
            MDC.put(MDC_CORRELATION_ID_KEY, correlationId);

            // Add to response header
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Continue with the request
            chain.doFilter(request, response);
        } finally {
            // Always clean up MDC to prevent memory leaks
            MDC.clear();
        }
    }
}