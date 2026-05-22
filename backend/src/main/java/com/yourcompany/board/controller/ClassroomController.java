package com.yourcompany.board.controller;

import com.yourcompany.board.dto.ApiResponse;
import com.yourcompany.board.dto.ClassroomCreateRequest;
import com.yourcompany.board.dto.ClassroomResponse;
import com.yourcompany.board.entity.Classroom;
import com.yourcompany.board.entity.User;
import com.yourcompany.board.repository.ClassroomRepository;
import com.yourcompany.board.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {
    private final ClassroomRepository classroomRepo;
    private final UserRepository userRepo;

    public ClassroomController(ClassroomRepository classroomRepo, UserRepository userRepo) {
        this.classroomRepo = classroomRepo;
        this.userRepo = userRepo;
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
}