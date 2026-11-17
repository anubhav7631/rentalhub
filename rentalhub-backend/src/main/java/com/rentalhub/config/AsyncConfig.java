package com.rentalhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;


@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Keep 2 threads always alive for email sending
        executor.setCorePoolSize(2);

        // Scale up to 5 threads under load (sending bursts of emails)
        executor.setMaxPoolSize(5);

        // Queue up to 25 email tasks before rejecting new ones
        executor.setQueueCapacity(25);

        // Thread names appear in logs: "email-async-1", "email-async-2"
        executor.setThreadNamePrefix("email-async-");

        // Wait for queued tasks to finish before shutting down the app
        executor.setWaitForTasksToCompleteOnShutdown(true);

        executor.initialize();
        return executor;
    }
}