package com.preppilot.dsa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    Optional<Problem> findBySlug(String slug);
    List<Problem> findByCategoryOrderByDifficultyAscIdAsc(ProblemCategory category);
    List<Problem> findByCategoryAndDifficultyOrderByIdAsc(ProblemCategory category, DifficultyTier difficulty);
    List<Problem> findByDifficultyOrderByIdAsc(DifficultyTier difficulty);
    List<Problem> findAllByOrderByCategoryAscDifficultyAscIdAsc();
}
