package com.auth.backend.repository;

import com.auth.backend.model.ApiAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiAccessLogRepository extends JpaRepository<ApiAccessLog, Long> {
    List<ApiAccessLog> findTop80ByOrderByCreatedAtDesc();
}
