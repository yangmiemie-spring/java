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
    private String aiUrl;

    private final RestTemplate restTemplate = new  RestTemplate();

    public Map<String, Object> predict(String payload){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"payload\":\"" + payload.replace("\"", "\\\"") + "\"}";
        HttpEntity<String> request = new HttpEntity<>(json, headers);
        return restTemplate.postForObject(aiUrl, request, Map.class);
    }

}
