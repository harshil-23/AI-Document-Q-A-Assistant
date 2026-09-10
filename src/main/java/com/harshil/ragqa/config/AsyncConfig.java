package com.harshil.ragqa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Dedicated thread pool for ingestion, separate from the web server's
 * request-handling threads. This is what "async ingestion" means in
 * practice: the upload endpoint returns as soon as the document is
 * persisted with status=PENDING, and chunking/embedding happens here,
 * off the request thread. In a real multi-instance deployment you'd
 * replace this in-process executor with an SQS queue + consumer so
 * ingestion survives an instance restart — this executor is the
 * single-instance version of that same idea.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "ingestionExecutor")
    public Executor ingestionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ingestion-");
        executor.initialize();
        return executor;
    }
}
