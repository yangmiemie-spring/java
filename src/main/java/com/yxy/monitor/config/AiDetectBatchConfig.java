package com.yxy.monitor.config;

import com.yxy.monitor.entity.WebAccessLog;
import com.yxy.monitor.processor.AiDetectProcessor;
import com.yxy.monitor.python.PythonAiClient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class AiDetectBatchConfig {

    @Bean
    public JdbcCursorItemReader<WebAccessLog> logReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<WebAccessLog>()
                .name("logReader")
                .dataSource(dataSource)
                .sql("SELECT id, request_url, ip_addr FROM web_access_log")
                .rowMapper(new BeanPropertyRowMapper<>(WebAccessLog.class))
                .build();
    }

    @Bean
    public AiDetectProcessor processor(PythonAiClient client) {
        return new AiDetectProcessor(client);
    }

    @Bean
    public JdbcBatchItemWriter<WebAccessLog> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<WebAccessLog>()
                .dataSource(dataSource)
                .sql("UPDATE web_access_log SET ai_label=?, ai_message=?, ai_score=? WHERE id=?")
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setInt(1, item.getAiLabel() == null ? 0 : item.getAiLabel());
                    ps.setString(2, item.getAiMessage() == null ? "异常" : item.getAiMessage());
                    ps.setDouble(3, item.getAiScore() == null ? 0.0 : item.getAiScore());
                    ps.setLong(4, item.getId());
                })
                .build();
    }

    @Bean
    public Step aiDetectStep(JobRepository jobRepository,
                             PlatformTransactionManager tx,
                             JdbcCursorItemReader<WebAccessLog> reader,
                             AiDetectProcessor processor,
                             JdbcBatchItemWriter<WebAccessLog> writer) {
        return new StepBuilder("aiDetectStep", jobRepository)
                .<WebAccessLog, WebAccessLog>chunk(10, tx)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job aiDetectJob(JobRepository jobRepository, Step aiDetectStep) {
        return new JobBuilder("aiDetectJob", jobRepository)
                .start(aiDetectStep)
                .build();
    }
}