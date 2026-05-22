package com.yourcompany.board.repository;

import com.yourcompany.board.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    List<Classroom> findByUserIdOrderByUpdatedAtDesc(Long userId);
}