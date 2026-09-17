package com.preppilot.design;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignQuestionRepository extends JpaRepository<DesignQuestion, Long> {
    Optional<DesignQuestion> findBySlug(String slug);
    List<DesignQuestion> findAllByOrderBySeniorityAscIdAsc();
    List<DesignQuestion> findBySeniorityOrderByIdAsc(SeniorityLevel seniority);
}
