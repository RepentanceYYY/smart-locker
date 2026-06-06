package com.tairui.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {
    @Bean("shelfPool")
    public ExecutorService shelfPool() {
        return new ThreadPoolExecutor(
                6,
                10,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("shelf-pool-" + System.currentTimeMillis());
                    return t;
                }
        );
    }
}
