package com.demo.myapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

/**
 * @Author: Yupeng Li
 * @Date: 13/9/2024 17:19
 * @Description:
 */

@Configuration
@EnableAsync
public class AsyncConfig {
    /**
     * 配置线程池
     * @return 线程池
     * Description: 配置线程池，用于异步发送邮件, 以及其他异步任务
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);      // 核心线程数
        executor.setMaxPoolSize(10);      // 最大线程数
        executor.setQueueCapacity(25);    // 队列容量
        executor.setThreadNamePrefix("taskExecutor-"); // 线程名前缀
        executor.initialize();
        return executor;
    }
}

