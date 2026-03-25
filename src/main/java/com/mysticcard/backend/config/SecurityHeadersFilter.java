package com.mysticcard.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Strict-Transport-Security (HSTS) - enforce HTTPS for 1 year including subdomains
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // X-Frame-Options - prevent Clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // X-XSS-Protection - prevent Cross-site scripting (Legacy but good practice)
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // X-Content-Type-Options - prevent MIME sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        filterChain.doFilter(request, response);
    }
}
