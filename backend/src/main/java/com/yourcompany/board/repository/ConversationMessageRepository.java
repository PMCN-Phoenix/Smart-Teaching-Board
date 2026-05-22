package com.yourcompany.board.repository;

import com.yourcompany.board.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
    List<ConversationMessage> findByClassroomIdOrderByTimestampAsc(Long classroomId);
}