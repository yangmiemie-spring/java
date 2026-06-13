package com.yxy.monitor.config;

import com.yxy.monitor.entity.WebAccessLog;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;

import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Date;
import java.util.Random;

@Configuration
public class BatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    public BatchConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager, DataSource dataSource) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.dataSource = dataSource;
    }

    // 多线程执行器 10线程并发插入
    @Bean
    public SimpleAsyncTaskExecutor batchTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setConcurrencyLimit(10);
        return executor;
    }

    // 模拟生成千万条测试访问日志

    public static class SafeLogGenerator implements ItemReader<WebAccessLog> {
        private static final long MAX_COUNT = 10000000;
        private long current = 0;
        private final Random random = new Random();
        private final String[] sqlPayloads = {
                "id=1", "id=1' or 1 = 1", "select * from user", "name = test or 1 = 1",
                "union select version()", "admin' --", "1=1 and 1=2", "or 1=1 limit 1"
        };
        private final String[] xssPayloads = {
                "<script>alert(1)</script>", "<img src=x onerror=alert(1)>",
                "javascript:alert(1)", "<svg onload=alert(1)>", "onclick=alert(1)",
                "onload=alert(1)", "<body onload=alert(1)>", "document.cookie"
        };
        private final String[] pathTraversalPayloads = {
                "../../etc/passwd", "../windows/system.ini", "..\\boot.ini",
                "%2E%2E/etc/passwd", "%252E%252E/boot.ini", "../../../../etc/passwd"
        };
        private final String[] scanPayloads = {
                "/admin/", "/phpmyadmin/", "/webshell/", "/shell.jsp", "/backup.sql",
                "/test.php", "/.git/config", "/.env", "/phpinfo.php", "/robots.txt"
        };
        private final String[] normalPaths = {
                "/api/login", "/api/register", "/api/user", "/api/post", "/api/comment"
        };

        private StepExecution stepExecution;

        @BeforeStep
        public void beforeStep(StepExecution stepExecution) {
            this.stepExecution = stepExecution;
            if(stepExecution.getExecutionContext().containsKey("current")) {
                current = stepExecution.getExecutionContext().getLong("current");
                System.out.println("断点续跑：从第 " + current + " 条开始");
            }
        }

        @Override
        public WebAccessLog read(){
            if(current >= MAX_COUNT){
                return null;
            }

            WebAccessLog webAccessLog = new WebAccessLog();
            webAccessLog.setIpAddr("192.168." + random.nextInt(255) + "." + random.nextInt(255));
            webAccessLog.setRequestMethod(random.nextBoolean()?"GET":"POST");

            int type = random.nextInt(5);
            String url;

            switch (type) {
                case 0: // 正常请求
                    url = normalPaths[random.nextInt(normalPaths.length)] + "?username=test&password=123456";
                    break;
                case 1: // SQL注入
                    url = normalPaths[random.nextInt(normalPaths.length)] + "?payload=" + sqlPayloads[random.nextInt(sqlPayloads.length)];
                    break;
                case 2: // XSS
                    url = normalPaths[random.nextInt(normalPaths.length)] + "?content=" + xssPayloads[random.nextInt(xssPayloads.length)];
                    break;
                case 3: // 路径遍历
                    url = pathTraversalPayloads[random.nextInt(pathTraversalPayloads.length)];
                    break;
                case 4: // 恶意扫描
                    url = scanPayloads[random.nextInt(scanPayloads.length)];
                    break;
                default:
                    url = normalPaths[0] + "?username=test";
            }
            webAccessLog.setRequestUrl(url);
            webAccessLog.setCreateTime(new Date());
            current++;

            if(current % 5000 == 0){
                double precent = current * 100.0 / MAX_COUNT;
                System.out.printf("已生成并写入： %d / %d (%.2f%%)%n", current, MAX_COUNT, precent);
            }

            stepExecution.getExecutionContext().put("current", current);

            return webAccessLog;
        }
    }
     @Bean
     public ItemReader<WebAccessLog> logItemReader(){
        return new SafeLogGenerator();
     }

    @Bean
    public JdbcBatchItemWriter<WebAccessLog> logWriter(){
        return new JdbcBatchItemWriterBuilder<WebAccessLog>()
                .dataSource(dataSource)
                .sql("INSERT INTO web_access_log(request_url, ip_addr, request_method, create_time) " +
                        "VALUES (:requestUrl, :ipAddr, :requestMethod, :createTime)")
                .beanMapped()
                .build();
    }

    @Bean
    public Step logBatchStep(ItemReader<WebAccessLog> logItemReader,
                             JdbcBatchItemWriter<WebAccessLog> logWriter){
        return new StepBuilder("logBatchStep", jobRepository)
                .<WebAccessLog, WebAccessLog>chunk(5000, transactionManager)
                .reader(logItemReader)
                .writer(logWriter)
                .taskExecutor(batchTaskExecutor())
                .build();
    }

    @Bean("accessLogCreateJob")
    public Job accessLogCreateJob(Step logBatchStep) {
        return new JobBuilder("accessLogCreateJob", jobRepository)
                .start(logBatchStep)
                .build();
    }
}
