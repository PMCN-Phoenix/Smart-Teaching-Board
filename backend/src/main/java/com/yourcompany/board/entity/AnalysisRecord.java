package com.yourcompany.board.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_records")
public class AnalysisRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT")
    private String imageBase64;

    private String whiteboardType;

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT")
    private String aiResult;

    private LocalDateTime createdAt = LocalDateTime.now();

    public AnalysisRecord() {}

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Classroom getClassroom() { return classroom; }
    public void setClassroom(Classroom classroom) { this.classroom = classroom; }
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public String getWhiteboardType() { return whiteboardType; }
    public void setWhiteboardType(String whiteboardType) { this.whiteboardType = whiteboardType; }
    public String getAiResult() { return aiResult; }
    public void setAiResult(String aiResult) { this.aiResult = aiResult; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}