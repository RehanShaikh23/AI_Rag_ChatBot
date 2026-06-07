package com.ragchatbot.config;

import com.ragchatbot.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user rate limiting filter for AI chat endpoints.
 * Uses Bucket4j token-bucket algorithm.
 */
@Component
@Order(1)
@Slf4j
public class RateLimitConfig implements Filter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.chat.capacity:30}")
    private int capacity;

    @Value("${app.rate-limit.chat.refill-tokens:30}")
    private int refillTokens;

    @Value("${app.rate-limit.chat.refill-duration-minutes:1}")
    private int refillMinutes;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // Only rate-limit AI chat endpoints
        if (path.startsWith("/api/chat")) {
            String userKey = getUserKey(httpRequest);
            Bucket bucket = buckets.computeIfAbsent(userKey, k -> createBucket());

            if (bucket.tryConsume(1)) {
                chain.doFilter(request, response);
            } else {
                log.warn("Rate limit exceeded for user: {}", userKey);
                throw new RateLimitExceededException(
                        "Too many requests. Please wait before sending more messages.");
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, Duration.ofMinutes(refillMinutes))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String getUserKey(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            // Use token hash as user key (avoids storing full tokens)
            return "user:" + Integer.toHexString(auth.hashCode());
        }
        return "anon:" + request.getRemoteAddr();
    }
}
