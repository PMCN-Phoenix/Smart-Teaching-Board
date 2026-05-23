package com.yourcompany.board.controller;

import com.yourcompany.board.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestExceptionController {

    @GetMapping("/runtime")
    public ResponseEntity<ApiResponse<String>> testRuntimeException() {
        // 故意抛出一个 RuntimeException，不捕获
        throw new RuntimeException("这是一个故意的运行时异常");
    }

    @GetMapping("/illegal-arg")
    public ResponseEntity<ApiResponse<String>> testIllegalArgumentException() {
        // 故意抛出一个 IllegalArgumentException
        throw new IllegalArgumentException("参数错误：id 不能为负数");
    }

    @GetMapping("/unknown")
    public ResponseEntity<ApiResponse<String>> testUnknownException() throws Exception {
        // 故意抛出一个受检异常
        throw new Exception("这是一个未预期的受检异常");
    }

    @GetMapping("/ok")
    public ResponseEntity<ApiResponse<String>> testOk() {
        // 正常响应对比
        return ResponseEntity.ok(ApiResponse.success("一切正常"));
    }
}