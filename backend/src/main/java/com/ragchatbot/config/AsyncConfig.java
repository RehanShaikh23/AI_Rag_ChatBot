package com.ragchatbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async thread pool configuration.
 * <p>
 * Controls the thread pool for:
 * <ul>
 *   <li>{@code @Async} tasks (document processing) — default executor</li>
 *   <li>{@code CompletableFuture} parallel work in ChatService — named "chatExecutor"</li>
 * </ul>
 * Prevents unbounded thread creation under load (Spring's default
 * SimpleAsyncTaskExecutor creates a new thread per task with no limit).
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Default executor for @Async — used by DocumentProcessor.
     * Core=2, Max=5, Queue=25 — suitable for moderate document upload traffic.
     */
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("doc-processor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Dedicated executor for ChatService parallel lookups
     * (history + RAG context fetched concurrently).
     */
    @Bean(name = "chatExecutor")
    public Executor chatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("chat-parallel-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }
}
