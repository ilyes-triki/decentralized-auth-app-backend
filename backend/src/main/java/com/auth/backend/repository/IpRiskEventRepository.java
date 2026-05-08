package com.auth.backend.repository;

import com.auth.backend.model.IpRiskEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IpRiskEventRepository extends JpaRepository<IpRiskEvent, Long> {

    List<IpRiskEvent> findTop200ByOrderByCreatedAtDesc();

    List<IpRiskEvent> findTop200ByRiskLevelOrderByCreatedAtDesc(String riskLevel);

    List<IpRiskEvent> findTop100ByIpOrderByCreatedAtDesc(String ip);
}
