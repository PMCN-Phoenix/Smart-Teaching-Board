package com.yourcompany.board.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AnalyzeRequest {

    @JsonProperty("image_base64")
    private String imageBase64;

    @JsonProperty("whiteboard_type")
    private String whiteboardType;

    private String context;

    @JsonProperty("strict_format")
    private Boolean strictFormat;   // 对应 AI 服务的 strict_format 参数

    // Getter & Setter（可用 IDE 生成，或直接复制以下代码）
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public String getWhiteboardType() { return whiteboardType; }
    public void setWhiteboardType(String whiteboardType) { this.whiteboardType = whiteboardType; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public Boolean getStrictFormat() { return strictFormat; }
    public void setStrictFormat(Boolean strictFormat) { this.strictFormat = strictFormat; }
}