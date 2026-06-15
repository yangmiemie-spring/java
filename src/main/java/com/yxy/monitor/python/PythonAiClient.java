package com.yxy.monitor.python;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PythonAiClient {
    @Value("${ai.service.url}")
    private String batchUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PythonAiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Map<String, Object>> batchDetect(List<String> urlList){
        if(urlList == null || urlList.isEmpty()){
            return List.of();
        }

        System.out.println("==== 批量检测开始，批次大小：" + urlList.size() + " ====");

        try{
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, List<String>> requestBody = new HashMap<>();
            requestBody.put("url_list", urlList);

            HttpEntity<Map<String, List<String>>> requestEntity = new HttpEntity<>(requestBody, headers);
            String responseBody = restTemplate.postForObject(batchUrl, requestEntity, String.class);

            List<Map<String, Object>> results = objectMapper.readValue(
                    responseBody,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            System.out.println("==== 批量检测完成，返回结果数：" + results.size() + " ====");
            return results;
        } catch(Exception e){
            System.out.println("==== 批量检测接口调用失败:" + e.getMessage() + " ====");
            return List.of();
        }
    }

}
