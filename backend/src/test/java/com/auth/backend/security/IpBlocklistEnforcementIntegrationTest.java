package com.auth.backend.security;

import com.auth.backend.service.IpBlocklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IpBlocklistEnforcementIntegrationTest {

    private static final String WALLET = "0xabc1234567890abcdef1234567890abcdef12";
    private static final String BLOCKED_IP = "203.0.113.70";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IpBlocklistService ipBlocklistService;

    private static RequestPostProcessor remoteAddr(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    @Test
    void blockedIpCannotRequestNonce() throws Exception {
        ipBlocklistService.adminBlock(BLOCKED_IP, "integration test", null, "admin");

        mockMvc.perform(get("/api/auth/nonce")
                        .param("address", WALLET)
                        .with(remoteAddr(BLOCKED_IP)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("IP_BLOCKED"));
    }

    @Test
    void blockedIpCannotLogin() throws Exception {
        ipBlocklistService.adminBlock(BLOCKED_IP, "integration test", null, "admin");

        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddr(BLOCKED_IP))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "address":"%s",
                                  "signature":"0xsig",
                                  "message":"nonce-msg"
                                }
                                """.formatted(WALLET)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("IP_BLOCKED"));
    }

    @Test
    void unblockedIpCanRequestNonceAgain() throws Exception {
        ipBlocklistService.adminBlock(BLOCKED_IP, "integration test", null, "admin");
        ipBlocklistService.unblock(BLOCKED_IP);

        mockMvc.perform(get("/api/auth/nonce")
                        .param("address", WALLET)
                        .with(remoteAddr(BLOCKED_IP)))
                .andExpect(status().isOk());
    }

    @Test
    void loopbackBlockDeniesNonceForIpv6RemoteAddr() throws Exception {
        ipBlocklistService.adminBlock("127.0.0.1", "local block", null, "admin");

        mockMvc.perform(get("/api/auth/nonce")
                        .param("address", WALLET)
                        .with(remoteAddr("0:0:0:0:0:0:0:1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("IP_BLOCKED"));
    }

    @Test
    void blockedIpCannotAccessProtectedProfileRoute() throws Exception {
        ipBlocklistService.adminBlock(BLOCKED_IP, "integration test", null, "admin");

        mockMvc.perform(get("/api/profile/me")
                        .with(remoteAddr(BLOCKED_IP)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("IP_BLOCKED"));
    }
}
