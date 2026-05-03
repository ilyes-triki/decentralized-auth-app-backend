package com.auth.backend.config;

import com.auth.backend.security.ApiAccessLogFilter;
import com.auth.backend.service.ApiAccessLogService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditFilterConfig {

    @Bean
    public ApiAccessLogFilter apiAccessLogFilter(ApiAccessLogService apiAccessLogService) {
        return new ApiAccessLogFilter(apiAccessLogService);
    }
}
