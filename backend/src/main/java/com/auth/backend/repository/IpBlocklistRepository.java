package com.auth.backend.repository;

import com.auth.backend.model.IpBlocklistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IpBlocklistRepository extends JpaRepository<IpBlocklistEntry, String> {

    List<IpBlocklistEntry> findAllByOrderByCreatedAtDesc();
}
