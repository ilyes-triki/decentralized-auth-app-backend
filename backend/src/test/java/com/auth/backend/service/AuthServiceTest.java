package com.auth.backend.service;

import com.auth.backend.model.UserAccount;
import com.auth.backend.repository.AuthNonceRepository;
import com.auth.backend.repository.LoginHistoryRepository;
import com.auth.backend.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private AuthService newAuthService(String adminWallets) {
        AuthNonceRepository authNonceRepository = mock(AuthNonceRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        LoginHistoryRepository loginHistoryRepository = mock(LoginHistoryRepository.class);
        return new AuthService(
                authNonceRepository,
                userAccountRepository,
                loginHistoryRepository,
                adminWallets,
                300
        );
    }

    @Test
    void getRoleReturnsAdminForConfiguredWalletIgnoringCase() {
        AuthService authService = newAuthService(
                "0x97550031867e9483c6f9cff121e683eddeac6f5e,0xabc123"
        );

        String role = authService.getRole("0x97550031867E9483c6F9Cff121e683EdDeAC6f5E");

        assertEquals("admin", role);
    }

    @Test
    void getRoleReturnsAdminForSecondConfiguredWallet() {
        AuthService authService = newAuthService(
                "0x97550031867e9483c6f9cff121e683eddeac6f5e,0xabc123"
        );

        String role = authService.getRole("0xAbC123");

        assertEquals("admin", role);
    }

    @Test
    void getRoleReturnsUserWhenWalletNotConfigured() {
        AuthService authService = newAuthService("0x97550031867e9483c6f9cff121e683eddeac6f5e");

        String role = authService.getRole("0xnotadmin");

        assertEquals("user", role);
    }

    @Test
    void getRoleReturnsUserForNullAddress() {
        AuthService authService = newAuthService("0x97550031867e9483c6f9cff121e683eddeac6f5e");

        String role = authService.getRole(null);

        assertEquals("user", role);
    }

    @Test
    void getRoleReturnsStoredRoleWhenUserExistsInDatabase() {
        AuthNonceRepository authNonceRepository = mock(AuthNonceRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        LoginHistoryRepository loginHistoryRepository = mock(LoginHistoryRepository.class);

        UserAccount userAccount = new UserAccount();
        userAccount.setAddress("0xstored");
        userAccount.setRole("admin");
        when(userAccountRepository.findById("0xstored")).thenReturn(Optional.of(userAccount));

        AuthService authService = new AuthService(
                authNonceRepository,
                userAccountRepository,
                loginHistoryRepository,
                "",
                300
        );

        String role = authService.getRole("0xstored");

        assertEquals("admin", role);
    }
}
