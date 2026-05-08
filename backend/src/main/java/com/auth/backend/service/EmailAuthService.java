package com.auth.backend.service;

import com.auth.backend.model.EmailOtp;
import com.auth.backend.model.EmailTicket;
import com.auth.backend.repository.EmailOtpRepository;
import com.auth.backend.repository.EmailTicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class EmailAuthService {

    private static final Logger EMAIL_OTP_LOG = LoggerFactory.getLogger("EMAIL_OTP_DEV");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$"
    );

    private final EmailOtpRepository emailOtpRepository;
    private final EmailTicketRepository emailTicketRepository;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    private final long otpTtlSeconds;
    private final long ticketTtlSeconds;
    private final int maxOtpVerifyAttempts;
    private final String devFixedOtp;

    public EmailAuthService(
            EmailOtpRepository emailOtpRepository,
            EmailTicketRepository emailTicketRepository,
            @Value("${app.auth.email-otp-ttl-seconds:600}") long otpTtlSeconds,
            @Value("${app.auth.email-ticket-ttl-seconds:900}") long ticketTtlSeconds,
            @Value("${app.auth.email-otp-max-verify-attempts:5}") int maxOtpVerifyAttempts,
            @Value("${app.auth.dev-fixed-otp:}") String devFixedOtp
    ) {
        this.emailOtpRepository = emailOtpRepository;
        this.emailTicketRepository = emailTicketRepository;
        this.otpTtlSeconds = otpTtlSeconds;
        this.ticketTtlSeconds = ticketTtlSeconds;
        this.maxOtpVerifyAttempts = maxOtpVerifyAttempts;
        this.devFixedOtp = devFixedOtp != null ? devFixedOtp : "";
    }

    public long getTicketTtlSeconds() {
        return ticketTtlSeconds;
    }

    public String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }

    public void validateEmailFormat(String email) {
        String n = normalizeEmail(email);
        if (n.isBlank() || !EMAIL_PATTERN.matcher(n).matches()) {
            throw new IllegalArgumentException("Invalid email address");
        }
    }

    @Transactional
    public void startEmailOtp(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        validateEmailFormat(email);

        String plain;
        if (!devFixedOtp.isBlank()) {
            plain = devFixedOtp.trim();
        } else {
            int otp = 100000 + random.nextInt(900000);
            plain = String.valueOf(otp);
        }
        String hash = bcrypt.encode(plain);

        EmailOtp row = new EmailOtp();
        row.setEmail(email);
        row.setOtpHash(hash);
        row.setExpiresAt(Instant.now().plusSeconds(otpTtlSeconds));
        row.setAttempts(0);
        emailOtpRepository.save(row);

        EMAIL_OTP_LOG.info("DEV_MOCK email={} otp={} (expires in {}s)", email, plain, otpTtlSeconds);
    }

    @Transactional
    public String verifyEmailOtpAndIssueTicket(String rawEmail, String rawOtp) {
        String email = normalizeEmail(rawEmail);
        validateEmailFormat(email);
        if (rawOtp == null || rawOtp.isBlank()) {
            throw new IllegalArgumentException("OTP is required");
        }

        EmailOtp row = emailOtpRepository.findFirstByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("No verification code found for this email"));

        if (row.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Verification code expired");
        }
        if (row.getAttempts() >= maxOtpVerifyAttempts) {
            throw new IllegalArgumentException("Too many incorrect attempts; request a new code");
        }

        if (!bcrypt.matches(rawOtp.trim(), row.getOtpHash())) {
            row.setAttempts(row.getAttempts() + 1);
            emailOtpRepository.save(row);
            throw new IllegalArgumentException("Invalid verification code");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        EmailTicket ticket = new EmailTicket();
        ticket.setToken(token);
        ticket.setEmail(email);
        ticket.setExpiresAt(Instant.now().plusSeconds(ticketTtlSeconds));
        emailTicketRepository.save(ticket);

        return token;
    }

    @Transactional(readOnly = true)
    public Optional<EmailTicket> findValidTicket(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return emailTicketRepository.findByTokenAndConsumedAtIsNull(token)
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()));
    }

    @Transactional
    public void consumeTicket(String token) {
        emailTicketRepository.findById(token).ifPresent(t -> {
            t.setConsumedAt(Instant.now());
            emailTicketRepository.save(t);
        });
    }
}
