package com.mysticcard.backend.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Ensures /api/stripe/webhook receives the raw (unparsed) request body,
 * which Stripe requires for signature verification.
 */
@Configuration
public class StripeWebhookConfig {

    @Bean
    public FilterRegistrationBean<RawBodyCachingFilter> stripeWebhookFilter() {
        FilterRegistrationBean<RawBodyCachingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RawBodyCachingFilter());
        registration.addUrlPatterns("/api/stripe/webhook");
        registration.setOrder(1);
        return registration;
    }

    public static class RawBodyCachingFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            byte[] body = httpRequest.getInputStream().readAllBytes();
            chain.doFilter(new CachedBodyHttpServletRequest(httpRequest, body), response);
        }
    }

    public static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.cachedBody = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new CachedBodyServletInputStream(cachedBody);
        }
    }

    public static class CachedBodyServletInputStream extends ServletInputStream {
        private final InputStream stream;

        public CachedBodyServletInputStream(byte[] body) {
            this.stream = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            try { return stream.available() == 0; } catch (IOException e) { return true; }
        }

        @Override
        public boolean isReady() { return true; }

        @Override
        public void setReadListener(ReadListener listener) {}

        @Override
        public int read() throws IOException { return stream.read(); }
    }
}
