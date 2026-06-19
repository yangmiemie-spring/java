package com.yxy.monitor.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 捕获DTO @Valid 参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validError(MethodArgumentNotValidException e) {
        Map<String, String> errorMap = new HashMap<>();
        for (FieldError err : e.getBindingResult().getFieldErrors()) {
            errorMap.put(err.getField(), err.getDefaultMessage());
        }
        // 返回 400 错误，携带校验提示，不会再走到403拦截
        return new ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
    }

    // 捕获业务手动抛出的异常（登录密码错误、用户名重复）
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> runtimeError(RuntimeException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
}