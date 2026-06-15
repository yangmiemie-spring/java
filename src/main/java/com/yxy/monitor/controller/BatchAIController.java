package com.yxy.monitor.controller;

import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BatchAIController {
    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private Job aiDetectJob;

    private volatile boolean jobRunning = false;

    @GetMapping("/start-ai-detect")
    public String startJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        if (jobRunning) {
            return "⚠️ 任务正在运行中，请勿重复触发！";
        }
        jobRunning = true;
        new Thread(() -> {
            try{
                JobParameters jobParameters = new JobParametersBuilder()
                        .addLong("time",System.currentTimeMillis())
                        .toJobParameters();
                jobLauncher.run(aiDetectJob, jobParameters);
                System.out.println("全部数据检测完成！");
            } catch(Exception e){
                System.out.println("任务执行出错：" + e.getMessage());
                e.printStackTrace();
            }finally {
                jobRunning = false;
            }
        }).start();
        return "批量AI检测任务已启动：正在后台运行中......";
    }
}
