# ai-service/server.py
import base64
import logging
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import Optional, List
from ai_analyzer import ZhipuVisionAnalyzer, WhiteboardType, AIResponse

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("AIServer")

app = FastAPI(title="云境智绘 AI 服务")

# 从环境变量读取 API Key，未设置时使用开发用 Key
import os
API_KEY = os.environ.get("ZHIPU_API_KEY", "899b1cee727b455a8c1b99464223e395.Qpzgo6tGVrQzlHdd")
analyzer = ZhipuVisionAnalyzer(API_KEY)

# ----- 原有 analyze 相关模型 -----
class AnalyzeRequest(BaseModel):
    image_base64: str = Field(..., description="图片 Base64 编码（不含 data:image 前缀）")
    whiteboard_type: str = Field("general", description="内容类型：code, math, physics, general")
    context: Optional[str] = Field("", description="附加上下文，如课程名称")
    strict_format: Optional[bool] = Field(True, description="是否启用强制 JSON 输出")

class AnalyzeResponse(BaseModel):
    success: bool
    content: str
    error_msg: Optional[str] = None
    cost_time: float
    model_used: str

@app.post("/analyze", response_model=AnalyzeResponse)
async def analyze(request: AnalyzeRequest):
    try:
        type_map = {
            "code": WhiteboardType.CODE,
            "math": WhiteboardType.MATH,
            "physics": WhiteboardType.PHYSICS,
            "general": WhiteboardType.GENERAL
        }
        wb_type = type_map.get(request.whiteboard_type, WhiteboardType.GENERAL)
        response: AIResponse = analyzer.analyze_image(
            image_base64=request.image_base64,
            whiteboard_type=wb_type,
            context=request.context,
            strict_format=request.strict_format
        )
        return AnalyzeResponse(
            success=response.success,
            content=response.content,
            error_msg=response.error_msg,
            cost_time=response.cost_time,
            model_used=response.model_used
        )
    except Exception as e:
        logger.exception("AI 服务内部异常")
        raise HTTPException(status_code=500, detail=f"AI 服务内部错误：{str(e)}")

# ----- 新增多轮对话模型与端点 -----
class ConversationMessage(BaseModel):
    role: str
    content: str

class ConversationRequest(BaseModel):
    messages: List[ConversationMessage]
    context: Optional[str] = ""

@app.post("/conversation")
async def conversation(request: ConversationRequest):
    try:
        payload = {
            "model": analyzer.model,
            "messages": [msg.dict() for msg in request.messages],
            "temperature": 0.1,
            "max_tokens": 2048
        }
        success, content, error = analyzer._call_api_with_retry(payload)
        return {"success": success, "content": content, "error_msg": error}
    except Exception as e:
        logger.exception("对话服务异常")
        raise HTTPException(status_code=500, detail=str(e))

# ----- 启动入口 -----
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)