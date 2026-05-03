package com.auth.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwtSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void profileEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/profile/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void profileEndpointReturnsAddressWhenTokenProvided() throws Exception {
        String token = jwtService.generateToken("0xabc", "user");

        mockMvc.perform(get("/api/profile/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("0xabc"));
    }

    @Test
    void adminEndpointRejectsUserRoleToken() throws Exception {
        String token = jwtService.generateToken("0xabc", "user");

        mockMvc.perform(get("/api/admin/health")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointAllowsAdminRoleToken() throws Exception {
        String token = jwtService.generateToken("0xadmin", "admin");

        mockMvc.perform(get("/api/admin/health")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.scope").value("admin"));
    }

    @Test
    void adminLoginHistoryEndpointAllowsAdminRoleToken() throws Exception {
        String token = jwtService.generateToken("0xadmin", "admin");

        mockMvc.perform(get("/api/admin/login-history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void adminStatsEndpointAllowsAdminRoleToken() throws Exception {
        String token = jwtService.generateToken("0xadmin", "admin");

        mockMvc.perform(get("/api/admin/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttempts").exists())
                .andExpect(jsonPath("$.successfulAttempts").exists())
                .andExpect(jsonPath("$.failedAttempts").exists());
    }

    @Test
    void adminStatsEndpointRejectsUserRoleToken() throws Exception {
        String token = jwtService.generateToken("0xabc", "user");

        mockMvc.perform(get("/api/admin/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminStatsWithSinceQueryAllowsAdminToken() throws Exception {
        String token = jwtService.generateToken("0xadmin", "admin");

        mockMvc.perform(get("/api/admin/stats")
                        .param("since", "2000-01-01T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttempts").exists());
    }

    @Test
    void adminStatsWithInvalidSinceReturnsBadRequest() throws Exception {
        String token = jwtService.generateToken("0xadmin", "admin");

        mockMvc.perform(get("/api/admin/stats")
                        .param("since", "not-a-date")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void adminAccessLogEndpointAllowsAdminRoleToken() throws Exception {
        String token = jwtService.generateToken("0xadmin", "admin");

        mockMvc.perform(get("/api/admin/access-log")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void adminAccessLogEndpointRejectsUserRoleToken() throws Exception {
        String token = jwtService.generateToken("0xabc", "user");

        mockMvc.perform(get("/api/admin/access-log")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
