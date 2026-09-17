package com.preppilot.subscription;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageService {

    private final UsageCounterRepository counters;

    public UsageService(UsageCounterRepository counters) {
        this.counters = counters;
    }

    @Transactional
    public UsageCounter recordDsaSession(Long userId) {
        UsageCounter c = today(userId);
        c.incrementDsa();
        return counters.save(c);
    }

    @Transactional
    public UsageCounter recordDesignSession(Long userId) {
        UsageCounter c = today(userId);
        c.incrementDesign();
        return counters.save(c);
    }

    @Transactional(readOnly = true)
    public UsageCounter today(Long userId) {
        LocalDate today = LocalDate.now();
        return counters.findByUserIdAndUsageDate(userId, today)
                .orElseGet(() -> new UsageCounter(userId, today));
    }
}
