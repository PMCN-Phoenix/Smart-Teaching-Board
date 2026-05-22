package com.yourcompany.board.repository;

import com.yourcompany.board.entity.AnalysisRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnalysisRecordRepository extends JpaRepository<AnalysisRecord, Long> {
    List<AnalysisRecord> findByClassroomIdOrderByCreatedAtDesc(Long classroomId);
}