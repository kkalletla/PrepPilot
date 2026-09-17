package com.preppilot.subscription;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageCounterRepository extends JpaRepository<UsageCounter, Long> {
    Optional<UsageCounter> findByUserIdAndUsageDate(Long userId, LocalDate usageDate);
    List<UsageCounter> findByUserIdAndUsageDateBetween(Long userId, LocalDate from, LocalDate to);
}
