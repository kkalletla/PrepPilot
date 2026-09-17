package com.preppilot.dsa;

import jakarta.persistence.*;

@Entity
@Table(name = "problems")
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProblemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DifficultyTier difficulty;

    @Column(nullable = false, length = 4000)
    private String statement;

    protected Problem() {}

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public ProblemCategory getCategory() { return category; }
    public DifficultyTier getDifficulty() { return difficulty; }
    public String getStatement() { return statement; }
}
