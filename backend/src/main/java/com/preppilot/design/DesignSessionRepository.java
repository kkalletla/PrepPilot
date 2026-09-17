package com.preppilot.design;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignSessionRepository extends JpaRepository<DesignSession, Long> {
    Optional<DesignSession> findByIdAndUserId(Long id, Long userId);
    List<DesignSession> findByUserIdOrderByStartedAtDesc(Long userId);
}
