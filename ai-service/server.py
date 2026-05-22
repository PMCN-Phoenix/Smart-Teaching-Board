# ai-service/server.py
import base64
import logging
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import Optional
from ai_analyzer import ZhipuVisionAnalyzer, WhiteboardType, AIResponse

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("AIServer")

app = FastAPI(title="云境智绘 AI 服务")

# 从环境变量读取 API Key，未设置时使用开发用 Key（正式上线前务必替换为环境变量）
import os
API_KEY = os.environ.get("ZHIPU_API_KEY", "899b1cee727b455a8c1b99464223e395.Qpzgo6tGVrQzlHdd")
analyzer = ZhipuVisionAnalyzer(API_KEY)

class AnalyzeRequest(BaseModel):
    image_base64: str = Field(..., description="图片 Base64 编码（不含 data:image 前缀）")
    whiteboard_type: str = Field("general", description="内容类型：code, math, physics, general")
    context: Optional[str] = Field("", description="附加上下文，如课程名称")
    strict_format: Optional[bool] = Field(True, description="是否启用强制 JSON 输出（手写识别建议开启）")

class AnalyzeResponse(BaseModel):
    success: bool
    content: str               # 当 strict_format=True 时这里是 JSON 字符串
    error_msg: Optional[str] = None
    cost_time: float
    model_used: str

@app.post("/analyze", response_model=AnalyzeResponse)
async def analyze(request: AnalyzeRequest):
    try:
        # 白板类型映射
        type_map = {
            "code": WhiteboardType.CODE,
            "math": WhiteboardType.MATH,
            "physics": WhiteboardType.PHYSICS,
            "general": WhiteboardType.GENERAL
        }
        wb_type = type_map.get(request.whiteboard_type, WhiteboardType.GENERAL)

        # 调用分析器（传递 strict_format 参数）
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

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)