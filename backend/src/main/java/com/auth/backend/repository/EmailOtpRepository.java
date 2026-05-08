package com.auth.backend.repository;

import com.auth.backend.model.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp> findFirstByEmailOrderByCreatedAtDesc(String email);
}
