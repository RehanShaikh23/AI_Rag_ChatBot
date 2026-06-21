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
 * Per-user rate limiting filter for AI chat and document upload endpoints.
 * Uses Bucket4j token-bucket algorithm with separate limits per endpoint group.
 */
@Component
@Order(1)
@Slf4j
public class RateLimitConfig implements Filter {

    private final Map<String, Bucket> chatBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> docBuckets = new ConcurrentHashMap<>();

    // ---- Chat rate limits ----
    @Value("${app.rate-limit.chat.capacity:30}")
    private int chatCapacity;

    @Value("${app.rate-limit.chat.refill-tokens:30}")
    private int chatRefillTokens;

    @Value("${app.rate-limit.chat.refill-duration-minutes:1}")
    private int chatRefillMinutes;

    // ---- Document upload rate limits ----
    @Value("${app.rate-limit.document.capacity:10}")
    private int docCapacity;

    @Value("${app.rate-limit.document.refill-tokens:10}")
    private int docRefillTokens;

    @Value("${app.rate-limit.document.refill-duration-minutes:5}")
    private int docRefillMinutes;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // Rate-limit AI chat endpoints
        if (path.startsWith("/api/chat")) {
            applyRateLimit(httpRequest, chatBuckets, this::createChatBucket, "chat");
            chain.doFilter(request, response);
        }
        // Rate-limit document upload endpoints
        else if (path.startsWith("/api/documents/upload")) {
            applyRateLimit(httpRequest, docBuckets, this::createDocBucket, "document upload");
            chain.doFilter(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private void applyRateLimit(HttpServletRequest request, Map<String, Bucket> buckets,
                                 java.util.function.Supplier<Bucket> bucketFactory, String endpointType) {
        String userKey = getUserKey(request);
        Bucket bucket = buckets.computeIfAbsent(userKey, k -> bucketFactory.get());

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for {} — user: {}", endpointType, userKey);
            throw new RateLimitExceededException(
                    "Too many requests. Please wait before sending more " + endpointType + " requests.");
        }
    }

    private Bucket createChatBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(chatCapacity)
                .refillGreedy(chatRefillTokens, Duration.ofMinutes(chatRefillMinutes))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createDocBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(docCapacity)
                .refillGreedy(docRefillTokens, Duration.ofMinutes(docRefillMinutes))
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

