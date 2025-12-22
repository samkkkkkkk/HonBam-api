package com.example.HonBam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "chatTaskExecutor")
    public Executor chatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본적으로 유지할 스레드 수
        executor.setCorePoolSize(10);

        // 최대 스레드 수
        executor.setMaxPoolSize(50);

        // 대기열 크기
        executor.setQueueCapacity(1000);

        // 스레드 이름 접두사
        executor.setThreadNamePrefix("chat-async-");

        // 거부정책
        // AbortPolicy(기본값) - 에러를 뱉고 작업을 버린다.
        // CallerRunsPolicy - 요청자에게 일을 시킨다.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }
}
