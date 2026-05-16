package com.corntrol.corntrol.domain.focus.repository;

import com.corntrol.corntrol.domain.focus.entity.FocusSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {
    void deleteByUserId(Long userId);
}
