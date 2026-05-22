package com.yourcompany.board.controller;

import com.yourcompany.board.dto.AnalyzeRequest;
import com.yourcompany.board.dto.AnalyzeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
public class AiController {

    //@Value("${ai.service.url}")
    //private String aiServiceUrl;

    private String aiServiceUrl = "http://localhost:8000/analyze";
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeResponse> analyze(@RequestBody AnalyzeRequest request) {
        try {
            // 1. 准备转发请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 2. 构建请求实体（包含请求体和头）
            HttpEntity<AnalyzeRequest> entity = new HttpEntity<>(request, headers);

            // 3. 调用 AI 微服务
            ResponseEntity<AnalyzeResponse> aiResp = restTemplate.exchange(
                    aiServiceUrl,
                    HttpMethod.POST,
                    entity,
                    AnalyzeResponse.class
            );

            // 4. 返回 AI 服务的结果
            return ResponseEntity.ok(aiResp.getBody());
        } catch (Exception e) {
            // 5. 异常处理：返回统一错误结构
            AnalyzeResponse errorResp = new AnalyzeResponse();
            errorResp.setSuccess(false);
            errorResp.setErrorMsg("后端调用 AI 服务失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResp);
        }
    }
}