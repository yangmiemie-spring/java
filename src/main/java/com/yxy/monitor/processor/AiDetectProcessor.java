package com.yxy.monitor.processor;

import com.yxy.monitor.entity.WebAccessLog;
import com.yxy.monitor.python.PythonAiClient;
import org.springframework.batch.item.ItemProcessor;

public class AiDetectProcessor implements ItemProcessor<WebAccessLog, WebAccessLog> {
    private final PythonAiClient pythonAiClient;

    public AiDetectProcessor(PythonAiClient pythonAiClient) {
        this.pythonAiClient = pythonAiClient;
    }

    @Override
    public WebAccessLog process(WebAccessLog webAccessLog) throws Exception {
        try{
            System.out.println("正在检测：" + webAccessLog.getRequestUrl());
            var result = pythonAiClient.predict(webAccessLog.getRequestUrl());
            webAccessLog.setAiLabel((Integer) result.get("label"));
            webAccessLog.setAiMessage((String)result.get("message"));
            webAccessLog.setAiScore((Double)result.get("ai_score"));
            System.out.println("AI结果：" + result);
        } catch (Exception e) {
            e.printStackTrace();

            webAccessLog.setAiLabel(0);
            webAccessLog.setAiMessage("AI检测异常");
            webAccessLog.setAiScore(0.0);
        }
        return webAccessLog;
    }
}
