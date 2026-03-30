package cc.felixzoe.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * API Security Headers Filter
 * Adds comprehensive security headers to all API responses
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Prevent MIME-type sniffing
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // Prevent clickjacking
        httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");

        // XSS Protection (legacy browsers)
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // Referrer policy
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Content Security Policy for API
        httpResponse.setHeader("Content-Security-Policy",
            "default-src 'none'; frame-ancestors 'none'");

        // Permissions policy - restrict dangerous APIs
        httpResponse.setHeader("Permissions-Policy",
            "camera=(), microphone=(), geolocation=(), payment=(), usb=(), bluetooth=()");

        // Remove server info
        httpResponse.setHeader("Server", "");

        // Prevent caching of sensitive API responses
        httpResponse.setHeader("X-Permitted-Cross-Domain-Policies", "none");

        // Cross-Origin policies - use cross-origin for API to allow frontend access
        httpResponse.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        // Allow cross-origin resource access for frontend API calls
        httpResponse.setHeader("Cross-Origin-Resource-Policy", "cross-origin");

        // Strict Transport Security (HTTPS only)
        httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        chain.doFilter(request, response);
    }
}
