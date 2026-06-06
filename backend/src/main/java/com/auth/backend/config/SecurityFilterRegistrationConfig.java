package com.auth.backend.config;

import com.auth.backend.security.AccountStatusFilter;
import com.auth.backend.security.AuthRateLimitFilter;
import com.auth.backend.security.IpBlocklistFilter;
import com.auth.backend.security.IpRiskAnalysisFilter;
import com.auth.backend.security.JwtAuthenticationFilter;
import com.auth.backend.security.RequestIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Security filters are registered only on {@link com.auth.backend.security.SecurityConfig}'s chain.
 * Disable servlet-container auto-registration for {@code @Component} filters so they cannot run
 * after the response is already committed.
 */
@Configuration
public class SecurityFilterRegistrationConfig {

    private static <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> disabled(T filter) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration(RequestIdFilter filter) {
        return disabled(filter);
    }

    @Bean
    FilterRegistrationBean<IpBlocklistFilter> ipBlocklistFilterRegistration(IpBlocklistFilter filter) {
        return disabled(filter);
    }

    @Bean
    FilterRegistrationBean<IpRiskAnalysisFilter> ipRiskAnalysisFilterRegistration(IpRiskAnalysisFilter filter) {
        return disabled(filter);
    }

    @Bean
    FilterRegistrationBean<AuthRateLimitFilter> authRateLimitFilterRegistration(AuthRateLimitFilter filter) {
        return disabled(filter);
    }

    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(JwtAuthenticationFilter filter) {
        return disabled(filter);
    }

    @Bean
    FilterRegistrationBean<AccountStatusFilter> accountStatusFilterRegistration(AccountStatusFilter filter) {
        return disabled(filter);
    }
}
