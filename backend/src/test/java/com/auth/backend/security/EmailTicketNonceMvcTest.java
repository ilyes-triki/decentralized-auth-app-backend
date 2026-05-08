package com.auth.backend.security;

import com.auth.backend.model.UserAccount;
import com.auth.backend.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmailTicketNonceMvcTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserAccountRepository userAccountRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void emailVerifyIssuesTicketThenNonceRequiresTicket() throws Exception {
        String email = "student+" + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/api/auth/email/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk());

        String json = mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"otp\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailTicket").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = objectMapper.readTree(json);
        String token = node.get("emailTicket").asText();

        mockMvc.perform(get("/api/auth/nonce")
                        .param("address", "0xabc1234567890abcdef1234567890abcdef12")
                        .param("emailTicket", token))
                .andExpect(status().isOk());
    }

    @Test
    void nonceAllowsWalletOnlyForAlreadyVerifiedWallet() throws Exception {
        String address = "0xabc1234567890abcdef1234567890abcdef12";
        UserAccount existing = new UserAccount();
        existing.setAddress(address);
        existing.setRole("user");
        existing.setEmail("verified@example.com");
        existing.setEmailVerified(true);
        userAccountRepository.save(existing);

        mockMvc.perform(get("/api/auth/nonce")
                        .param("address", address))
                .andExpect(status().isOk());
    }

    @Test
    void emailStatusReflectsLinkedVerifiedWallet() throws Exception {
        String address = "0x9991234567890abcdef1234567890abcdef12";
        UserAccount existing = new UserAccount();
        existing.setAddress(address);
        existing.setRole("user");
        existing.setEmail("known@example.com");
        existing.setEmailVerified(true);
        userAccountRepository.save(existing);

        mockMvc.perform(get("/api/auth/email-status")
                        .param("address", address))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.linked").value(true))
                .andExpect(jsonPath("$.accountBlocked").value(false))
                .andExpect(jsonPath("$.email").value("known@example.com"));
    }
}
