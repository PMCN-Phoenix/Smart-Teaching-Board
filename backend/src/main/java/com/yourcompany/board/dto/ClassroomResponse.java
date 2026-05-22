package com.yourcompany.board.dto;

import java.time.LocalDateTime;

public class ClassroomResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime createdAt;

    public ClassroomResponse(Long id, String title, String description, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}