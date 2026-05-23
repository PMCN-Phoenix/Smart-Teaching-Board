package com.yourcompany.board.controller;

import com.yourcompany.board.config.AiServiceConfig;
import com.yourcompany.board.dto.*;
import com.yourcompany.board.entity.*;
import com.yourcompany.board.repository.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/classrooms/{classroomId}")
public class ClassroomAiController {

    private final AiServiceConfig aiConfig;
    private final RestTemplate restTemplate;
    private final ClassroomRepository classroomRepo;
    private final AnalysisRecordRepository recordRepo;
    private final ConversationMessageRepository msgRepo;
    private final UserRepository userRepo;

    public ClassroomAiController(AiServiceConfig aiConfig,
                                 ClassroomRepository classroomRepo,
                                 AnalysisRecordRepository recordRepo,
                                 ConversationMessageRepository msgRepo,
                                 UserRepository userRepo) {
        this.aiConfig = aiConfig;
        this.restTemplate = new RestTemplate();
        this.classroomRepo = classroomRepo;
        this.recordRepo = recordRepo;
        this.msgRepo = msgRepo;
        this.userRepo = userRepo;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<AnalyzeResponse>> analyze(@PathVariable Long classroomId,
                                                                @RequestBody AnalyzeRequest request,
                                                                Principal principal) {
        // 权限校验：课堂必须属于当前用户
        User user = userRepo.findByUsername(principal.getName()).orElseThrow();
        Classroom classroom = classroomRepo.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("课堂不存在"));
        if (!classroom.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "无权操作该课堂"));
        }

        // 转发请求到 AI 微服务
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AnalyzeRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<AnalyzeResponse> aiResp = restTemplate.exchange(
                aiConfig.getAnalyzeUrl(), HttpMethod.POST, entity, AnalyzeResponse.class);

        AnalyzeResponse body = aiResp.getBody();
        if (body != null && body.isSuccess()) {
            // 保存分析记录
            AnalysisRecord record = new AnalysisRecord();
            record.setClassroom(classroom);
            record.setImageBase64(request.getImageBase64());
            record.setWhiteboardType(request.getWhiteboardType());
            record.setAiResult(body.getContent());
            recordRepo.save(record);

            // 初始化对话消息：用户（截图表征）和 AI 回复
            ConversationMessage userMsg = new ConversationMessage();
            userMsg.setClassroom(classroom);
            userMsg.setRole("user");
            userMsg.setContent("[发送了白板截图]");
            msgRepo.save(userMsg);

            ConversationMessage assistantMsg = new ConversationMessage();
            assistantMsg.setClassroom(classroom);
            assistantMsg.setRole("assistant");
            assistantMsg.setContent(body.getContent());
            msgRepo.save(assistantMsg);

            return ResponseEntity.ok(ApiResponse.success(body));
        } else {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "AI 分析失败"));
        }
    }

    @PostMapping("/conversation")
    public ResponseEntity<ApiResponse<Map<String, String>>> conversation(@PathVariable Long classroomId,
                                                                          @RequestBody Map<String, String> payload,
                                                                          Principal principal) {
        String userMessage = payload.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "消息不能为空"));
        }

        // 权限校验
        User user = userRepo.findByUsername(principal.getName()).orElseThrow();
        Classroom classroom = classroomRepo.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("课堂不存在"));
        if (!classroom.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "无权操作该课堂"));
        }

        // 构建完整对话历史
        List<ConversationMessage> history = msgRepo.findByClassroomIdOrderByTimestampAsc(classroomId);
        List<Map<String, String>> messages = new ArrayList<>();
        // 添加系统提示
        Map<String, String> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", "你是一位资深大学教授，正在回答学生针对白板内容的追问。请基于之前的分析结果进行解答。");
        messages.add(sysMsg);
        // 添加历史消息
        for (ConversationMessage msg : history) {
            Map<String, String> m = new HashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            messages.add(m);
        }
        // 添加用户新消息
        Map<String, String> newUserMsg = new HashMap<>();
        newUserMsg.put("role", "user");
        newUserMsg.put("content", userMessage);
        messages.add(newUserMsg);

        // 保存用户消息
        ConversationMessage userMsgEntity = new ConversationMessage();
        userMsgEntity.setClassroom(classroom);
        userMsgEntity.setRole("user");
        userMsgEntity.setContent(userMessage);
        msgRepo.save(userMsgEntity);

        // 调用 AI 多轮对话接口
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("messages", messages);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(aiRequest, headers);

        ResponseEntity<Map> resp = restTemplate.exchange(
                aiConfig.getConversationUrl(), HttpMethod.POST, entity, Map.class);

        if (resp.getStatusCode() == HttpStatus.OK && (boolean) resp.getBody().get("success")) {
            String aiContent = (String) resp.getBody().get("content");
            // 保存 AI 回复
            ConversationMessage assistantMsgEntity = new ConversationMessage();
            assistantMsgEntity.setClassroom(classroom);
            assistantMsgEntity.setRole("assistant");
            assistantMsgEntity.setContent(aiContent);
            msgRepo.save(assistantMsgEntity);

            Map<String, String> result = new HashMap<>();
            result.put("content", aiContent);
            return ResponseEntity.ok(ApiResponse.success(result));
        } else {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "AI 对话失败"));
        }
    }
}