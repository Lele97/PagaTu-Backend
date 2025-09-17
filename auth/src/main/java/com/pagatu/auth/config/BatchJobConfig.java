package com.pagatu.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class for batch job related beans and settings.
 */
@Configuration
@Slf4j
public class BatchJobConfig {

    /**
     * Custom task scheduler for batch jobs with appropriate thread pool configuration.
     */
    @Bean(name = "batchTaskScheduler")
    public TaskScheduler batchTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("batch-job-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        
        log.info("Initialized batch task scheduler with pool size: 2");
        return scheduler;
    }

    /**
     * Retry template with exponential backoff for database operations.
     */
    @Bean(name = "batchRetryTemplate")
    public RetryTemplate batchRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        log.info("Configured retry template with max 3 attempts and exponential backoff");
        return retryTemplate;
    }
}
