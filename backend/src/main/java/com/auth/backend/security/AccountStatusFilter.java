package com.auth.backend.security;

import com.auth.backend.dto.ApiErrorResponse;
import com.auth.backend.service.AccountStatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AccountStatusFilter extends OncePerRequestFilter {
    private final AccountStatusService accountStatusService;
    private final ObjectMapper objectMapper;

    public AccountStatusFilter(AccountStatusService accountStatusService, ObjectMapper objectMapper) {
        this.accountStatusService = accountStatusService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String principalAddress) {
            if (accountStatusService.isBlocked(principalAddress)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                objectMapper.writeValue(
                        response.getOutputStream(),
                        new ApiErrorResponse("Your account is blocked pending admin review.", HttpServletResponse.SC_FORBIDDEN, "ACCOUNT_BLOCKED")
                );
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
