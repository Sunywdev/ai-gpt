package com.sunyw.xyz.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;


/**
 * 线程池配置类
 */
@Configuration
@Slf4j
public class ThreadPoolConfig {

    @Bean("threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor init() {
        log.info("<===============================线程池初始化配置开始===============================>");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        /*
         * 核心线程数量
         */
        executor.setCorePoolSize(20);
        /*
         * 最大线程数量
         */
        executor.setMaxPoolSize(40);
        /*
         * 队列最大数量
         */
        executor.setQueueCapacity(Integer.MAX_VALUE);
        /*
         * 线程名称
         */
        executor.setThreadNamePrefix("taskThread----");
        /*
         * 线程空闲存活时间
         */
        executor.setKeepAliveSeconds(3000);
        /*
         * 设置线程超时回收
         */
        executor.setAllowCoreThreadTimeOut(true);
        /*
         * 拒绝策略
         */
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        /*
         * 初始化
         */
        executor.initialize();
        log.info("<===============================线程池初始化配置结束===============================>");
        return executor;
    }
}
