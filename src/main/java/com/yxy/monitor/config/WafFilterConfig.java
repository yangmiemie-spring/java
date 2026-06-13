package com.yxy.monitor.config;

import com.yxy.monitor.filter.WebAttackFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WafFilterConfig {
    @Bean
    public FilterRegistrationBean<WebAttackFilter> wafFilterRegister(WebAttackFilter wafFilter){
        FilterRegistrationBean<WebAttackFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(wafFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;

    }
}
