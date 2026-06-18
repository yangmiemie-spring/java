package com.yxy.monitor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.yxy.monitor.mapper")
@SpringBootApplication
@EnableScheduling
@EnableBatchProcessing
public class MonitorApplication {

    public static void main(String[] args) {

        SpringApplication.run(MonitorApplication.class, args);
    }

}
