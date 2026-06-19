package com.ragchatbot.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration — async support for SSE + request logging.
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    /**
     * Enable async request processing with a generous timeout for SSE streams.
     * SseEmitter relies on Servlet 3.1 async support.
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(300_000L); // 5 minutes, matches SseEmitter timeout
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestLoggingInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/chat/stream"); // SSE is long-lived, skip logging
    }

    /**
     * Logs every API request with method, URI, and response time.
     */
    static class RequestLoggingInterceptor implements HandlerInterceptor {

        private static final org.slf4j.Logger log =
                org.slf4j.LoggerFactory.getLogger(RequestLoggingInterceptor.class);

        @Override
        public boolean preHandle(HttpServletRequest request,
                                 HttpServletResponse response, Object handler) {
            request.setAttribute("startTime", System.currentTimeMillis());
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Object handler, Exception ex) {
            Object startAttr = request.getAttribute("startTime");
            if (startAttr == null) return; // async dispatch — skip
            long start = (long) startAttr;
            long duration = System.currentTimeMillis() - start;
            log.info("{} {} → {} ({}ms)",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);
        }
    }
}

