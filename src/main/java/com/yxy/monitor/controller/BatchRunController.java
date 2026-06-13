package com.yxy.monitor.controller;

import jakarta.annotation.Resource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


@RestController
public class BatchRunController {
    @Resource
    private JobLauncher jobLauncher;
    @Resource(name = "accessLogCreateJob")
    private Job accessLogCreateJob;

    @GetMapping("/batch/importLog")
    public String batchImport() throws Exception{
        try{
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("runTime", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(accessLogCreateJob,jobParameters);
            return "批量导入任务已启动，后台正在写入数据";
        } catch(Exception e){
            return "任务启动失败：" + e.getMessage();
        }

    }
}
