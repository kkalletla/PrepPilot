package com.preppilot.design;

import jakarta.persistence.*;

@Entity
@Table(name = "design_questions")
public class DesignQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 32)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SeniorityLevel seniority;

    @Column(nullable = false, length = 4000)
    private String prompt;

    protected DesignQuestion() {}

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public SeniorityLevel getSeniority() { return seniority; }
    public String getPrompt() { return prompt; }
}
