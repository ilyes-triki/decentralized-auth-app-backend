package com.auth.backend.repository;

import com.auth.backend.model.EmailTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailTicketRepository extends JpaRepository<EmailTicket, String> {

    Optional<EmailTicket> findByTokenAndConsumedAtIsNull(String token);
}
