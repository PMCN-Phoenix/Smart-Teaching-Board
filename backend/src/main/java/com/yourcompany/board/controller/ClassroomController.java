package com.yourcompany.board.controller;

import com.yourcompany.board.dto.ApiResponse;
import com.yourcompany.board.dto.ClassroomCreateRequest;
import com.yourcompany.board.dto.ClassroomResponse;
import com.yourcompany.board.entity.AnalysisRecord;
import com.yourcompany.board.entity.Classroom;
import com.yourcompany.board.entity.ConversationMessage;
import com.yourcompany.board.entity.User;
import com.yourcompany.board.repository.AnalysisRecordRepository;
import com.yourcompany.board.repository.ClassroomRepository;
import com.yourcompany.board.repository.ConversationMessageRepository;
import com.yourcompany.board.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {
    private final ClassroomRepository classroomRepo;
    private final UserRepository userRepo;
    private final AnalysisRecordRepository recordRepo;
    private final ConversationMessageRepository msgRepo;

    public ClassroomController(ClassroomRepository classroomRepo,
                               UserRepository userRepo,
                               AnalysisRecordRepository recordRepo,
                               ConversationMessageRepository msgRepo) {
        this.classroomRepo = classroomRepo;
        this.userRepo = userRepo;
        this.recordRepo = recordRepo;
        this.msgRepo = msgRepo;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClassroomResponse>> create(@Valid @RequestBody ClassroomCreateRequest req,
                                                                  Principal principal) {
        User user = userRepo.findByUsername(principal.getName()).orElseThrow();
        Classroom classroom = new Classroom(req.getTitle(), req.getDescription(), user);
        classroomRepo.save(classroom);
        ClassroomResponse response = new ClassroomResponse(
                classroom.getId(), classroom.getTitle(),
                classroom.getDescription(), classroom.getCreatedAt()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> list(Principal principal) {
        User user = userRepo.findByUsername(principal.getName()).orElseThrow();
        List<ClassroomResponse> list = classroomRepo.findByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(c -> new ClassroomResponse(c.getId(), c.getTitle(), c.getDescription(), c.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{classroomId}/records")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecords(
            @PathVariable Long classroomId, Principal principal) {
        User user = userRepo.findByUsername(principal.getName()).orElseThrow();
        Classroom classroom = classroomRepo.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("课堂不存在"));
        if (!classroom.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "无权操作该课堂"));
        }
        List<AnalysisRecord> records = recordRepo.findByClassroomIdOrderByCreatedAtDesc(classroomId);
        List<Map<String, Object>> result = records.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("whiteboardType", r.getWhiteboardType());
            map.put("aiResult", r.getAiResult());
            map.put("createdAt", r.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{classroomId}/messages")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMessages(
            @PathVariable Long classroomId, Principal principal) {
        User user = userRepo.findByUsername(principal.getName()).orElseThrow();
        Classroom classroom = classroomRepo.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("课堂不存在"));
        if (!classroom.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "无权操作该课堂"));
        }
        List<ConversationMessage> messages = msgRepo.findByClassroomIdOrderByTimestampAsc(classroomId);
        List<Map<String, Object>> result = messages.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("role", m.getRole());
            map.put("content", m.getContent());
            map.put("timestamp", m.getTimestamp());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}