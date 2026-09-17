package com.preppilot.dsa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemProgressRepository extends JpaRepository<ProblemProgress, Long> {
    Optional<ProblemProgress> findByUserIdAndProblemId(Long userId, Long problemId);
    List<ProblemProgress> findByUserIdOrderByLastAttemptAtDesc(Long userId);
    List<ProblemProgress> findByUserIdAndStatusOrderByLastAttemptAtDesc(Long userId, ProgressStatus status);
}
