package com.yourcompany.board.dto;

public class AnalyzeResponse {
    private boolean success;
    private String content;
    private String errorMsg;
    private double costTime;
    private String modelUsed;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public double getCostTime() { return costTime; }
    public void setCostTime(double costTime) { this.costTime = costTime; }

    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }
}