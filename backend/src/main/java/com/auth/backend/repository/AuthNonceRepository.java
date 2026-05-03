package com.auth.backend.repository;

import com.auth.backend.model.AuthNonce;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthNonceRepository extends JpaRepository<AuthNonce, String> {
}
