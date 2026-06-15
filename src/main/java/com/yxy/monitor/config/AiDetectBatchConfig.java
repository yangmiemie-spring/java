package com.yxy.monitor.config;

import com.yxy.monitor.entity.WebAccessLog;
import com.yxy.monitor.python.PythonAiClient;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableBatchProcessing
public class AiDetectBatchConfig {
    private static final int CHUNK_SIZE = 1000;

    @Autowired
    private PythonAiClient pythonAiClient;

    private final ThreadLocal<List<WebAccessLog>> chunkDataThreadLocal = ThreadLocal.withInitial(ArrayList::new);

    @Bean
    @StepScope
    public JdbcPagingItemReader<WebAccessLog> aiDetectLogReader(DataSource dataSource) {
        return new JdbcPagingItemReaderBuilder<WebAccessLog>()
                .name("aiDetectLogReader")
                .dataSource(dataSource)
                .queryProvider(queryProvider())
                .rowMapper((rs, rowNum) -> {
                    WebAccessLog log = new WebAccessLog();
                    log.setId(rs.getLong("id"));
                    log.setRequestUrl(rs.getString("request_url"));
                    return log;
                })
                .pageSize(CHUNK_SIZE)
                .build();
    }

    @Bean
    public PagingQueryProvider queryProvider() {
        MySqlPagingQueryProvider provider = new MySqlPagingQueryProvider();
        provider.setSelectClause("SELECT id, request_url");
        provider.setFromClause("FROM web_access_log");
        provider.setWhereClause("WHERE ai_label IS NULL");
        provider.setSortKeys(Collections.singletonMap("id", Order.ASCENDING));
        return provider;
    }

    // 固定UPDATE写入
    @Bean("aiUpdateLogWriter")
    public JdbcBatchItemWriter<WebAccessLog> aiUpdateLogWriter(DataSource dataSource) {
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
    public ItemWriteListener<WebAccessLog> aiDetectWriteListener() {
        return new ItemWriteListener<WebAccessLog>() {
            @Override
            public void beforeWrite(Chunk<? extends WebAccessLog> items) {
                List<? extends WebAccessLog> logList = items.getItems();
                if (logList.isEmpty()) {
                    return;
                }

                System.out.println("==== 本批次共 " + logList.size() + " 条，开始批量调用AI ====");

                // 提取所有URL，批量调用一次Python接口
                List<String> urlList = logList.stream()
                        .map(WebAccessLog::getRequestUrl)
                        .toList();

                List<Map<String, Object>> aiResults = pythonAiClient.batchDetect(urlList);
                System.out.println("==== AI调用完成，返回结果数：" + aiResults.size() + " ====");

                // 把AI结果映射回每条数据
                Map<String, Map<String, Object>> resultMap = new HashMap<>();
                for (Map<String, Object> result : aiResults) {
                    String url = (String) result.get("url");
                    if (url != null) {
                        resultMap.put(url, result);
                    }
                }

                for (WebAccessLog log : logList) {
                    Map<String, Object> result = resultMap.get(log.getRequestUrl());
                    if (result != null) {
                        log.setAiLabel((Integer) result.get("label"));
                        log.setAiMessage((String) result.get("message"));
                        log.setAiScore((Double) result.get("ai_score"));
                    } else {
                        log.setAiLabel(0);
                        log.setAiMessage("检测异常");
                        log.setAiScore(0.0);
                    }
                }
            }

            @Override
            public void afterWrite(Chunk<? extends WebAccessLog> items) {
                System.out.println("==== 本批次写入数据库完成 ====");
            }

            @Override
            public void onWriteError(Exception exception, Chunk<? extends WebAccessLog> items) {
                System.err.println("==== 批次写入出错：" + exception.getMessage() + " ====");
            }
        };
    }

    @Bean
    public Step aiDetectStep(JobRepository jobRepository,
                             PlatformTransactionManager tx,
                             JdbcPagingItemReader<WebAccessLog> aiDetectLogreader,
                             @Qualifier("aiUpdateLogWriter") JdbcBatchItemWriter<WebAccessLog> aiUpdateLogwriter) {

        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(8);

        return new StepBuilder("aiDetectStep", jobRepository)
                .<WebAccessLog, WebAccessLog>chunk(CHUNK_SIZE, tx)
                .reader(aiDetectLogreader)
                .processor(item -> {
                    chunkDataThreadLocal.get().add(item);
                    return item;
                })
                .writer(aiUpdateLogwriter)
                .listener(aiDetectWriteListener())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    public Job aiDetectJob(JobRepository jobRepository, Step aiDetectStep) {
        return new JobBuilder("aiDetectJob", jobRepository)
                .start(aiDetectStep)
                .build();
    }
}