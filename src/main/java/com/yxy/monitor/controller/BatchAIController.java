package com.yxy.monitor.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BatchAIController {
    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private Job aiDetectJob;

    @GetMapping("/start-ai-detect")
    public String startJob(){
        try{
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(aiDetectJob, params);
            return "AI检测任务已成功启动！请查看数据库ai_label字段。";
        } catch(Exception e){
            e.printStackTrace();
            return "启动失败：" + e.getMessage();
        }
    }
}
