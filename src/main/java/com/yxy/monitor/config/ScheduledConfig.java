package com.yxy.monitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
public class ScheduledConfig implements SchedulingConfigurer {
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 分配5个线程，多个定时任务并行执行，一个报错不影响其他
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(5));
    }
}