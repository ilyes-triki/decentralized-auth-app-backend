package com.auth.backend.repository;

import com.auth.backend.model.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    List<LoginHistory> findTop50ByOrderByCreatedAtDesc();

    List<LoginHistory> findTop50ByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(Instant since);

    long countBySuccessful(boolean successful);

    long count();

    @Query("SELECT COUNT(l) FROM LoginHistory l WHERE l.createdAt >= :since")
    long countAllSince(@Param("since") Instant since);

    @Query("SELECT COUNT(l) FROM LoginHistory l WHERE l.createdAt >= :since AND l.successful = true")
    long countSuccessfulSince(@Param("since") Instant since);

    @Query("SELECT COUNT(l) FROM LoginHistory l WHERE l.createdAt >= :since AND l.successful = false")
    long countFailedSince(@Param("since") Instant since);
}
