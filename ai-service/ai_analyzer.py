import base64
import requests
import time
import logging
import json
import re
from typing import Optional, Dict, Any, Tuple
from dataclasses import dataclass
from enum import Enum

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger("AIAnalyzer")


class WhiteboardType(Enum):
    """白板内容类型枚举"""
    CODE = "code"
    MATH = "math"
    PHYSICS = "physics"
    GENERAL = "general"


@dataclass
class AIResponse:
    """统一的AI响应数据结构"""
    success: bool
    content: str
    error_msg: Optional[str] = None
    cost_time: float = 0.0
    model_used: str = ""


class ZhipuVisionAnalyzer:
    """
    智谱 AI 视觉大模型分析器
    支持图片 Base64 输入，动态 Prompt 构建，自动重试与异常处理
    新增：强制 JSON 输出模式，手写连笔字专项优化
    """

    DEFAULT_MODEL = "glm-4v-plus"
    API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
    MAX_RETRIES = 2
    RETRY_DELAY = 1.5
    TIMEOUT = 25

    def __init__(self, api_key: str, model: str = DEFAULT_MODEL):
        self.api_key = api_key
        self.model = model
        self.session = requests.Session()
        self.session.headers.update({
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        })

    def analyze_image(self,
                      image_base64: str,
                      whiteboard_type: WhiteboardType = WhiteboardType.GENERAL,
                      custom_prompt: Optional[str] = None,
                      context: str = "",
                      strict_format: bool = True) -> AIResponse:
        """
        分析图片主入口
        
        :param image_base64: 图片 Base64 编码（不含 data:image 前缀）
        :param whiteboard_type: 白板内容类型
        :param custom_prompt: 自定义 Prompt，若提供则覆盖动态生成
        :param context: 附加上下文信息（如课程名称）
        :param strict_format: 是否强制要求返回 JSON 格式（手写测试建议开启）
        :return: AIResponse 对象
        """
        start_time = time.time()

        # 1. 构建 Prompt
        if custom_prompt:
            prompt = custom_prompt
        else:
            if strict_format:
                prompt = self._build_handwriting_prompt(whiteboard_type, context)
            else:
                prompt = self._build_dynamic_prompt(whiteboard_type, context)

        # 2. 构建请求体
        payload = {
            "model": self.model,
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": prompt},
                        {"type": "image_url", "image_url": {"url": image_base64}}
                    ]
                }
            ],
            "temperature": 0.1,
            "max_tokens": 2048
        }

        # 3. 带重试的 API 调用
        success, result_content, error_msg = self._call_api_with_retry(payload)

        cost_time = time.time() - start_time

        if not success:
            logger.error(f"AI 分析失败: {error_msg}")
            return AIResponse(
                success=False,
                content="",
                error_msg=error_msg,
                cost_time=cost_time,
                model_used=self.model
            )

        # 4. 如果需要严格 JSON 格式，则进行校验和修复
        if strict_format:
            ok, json_data, parse_error = self._extract_and_validate_json(result_content)
            if not ok:
                logger.warning(f"格式校验失败: {parse_error}，使用降级结果")
                # 降级：构造一个符合 schema 的假 JSON，内容为原始文本前500字符
                result_content = json.dumps({
                    "recognized_text": result_content[:500],
                    "confidence": 0.3,
                    "ambiguous_spots": ["格式解析失败，请人工核对"],
                    "structured_content": {
                        "type": whiteboard_type.value,
                        "body": result_content[:500]
                    }
                }, ensure_ascii=False)
            else:
                result_content = json.dumps(json_data, ensure_ascii=False)

        logger.info(f"AI 分析成功，耗时 {cost_time:.2f}s")
        return AIResponse(
            success=True,
            content=result_content,
            cost_time=cost_time,
            model_used=self.model
        )

    # ================= 新增：手写连笔字专用 Prompt（强制 JSON） =================
    def _build_handwriting_prompt(self, whiteboard_type: WhiteboardType, context: str = "") -> str:
        """
        专门用于手写识别的 Prompt —— 强制 JSON 输出，锁死格式
        """
        system_role = "你是一个严谨的手写板书识别专家。你的输出必须只包含一个 JSON 对象，不能有任何额外的文字说明或 Markdown 标记。"

        schema_example = """
{
  "recognized_text": "识别出的主要文本/代码/公式（转成 LaTeX 或纯文本）",
  "confidence": 0.92,
  "ambiguous_spots": ["第3行第2个字符疑似'5'或'3'"],
  "structured_content": {
    "type": "code | math | mixed",
    "body": "具体内容"
  }
}
"""
        if whiteboard_type == WhiteboardType.CODE:
            instruction = """
分析要求：
1. 将代码块放在 structured_content.body 中，保留缩进。
2. 若发现可能的语法错误或笔误，在 ambiguous_spots 中指出。
3. 对于完全无法辨认的字符，用 [?] 代替并在 ambiguous_spots 中说明位置。
"""
        elif whiteboard_type == WhiteboardType.MATH:
            instruction = """
分析要求：
1. 公式必须写成 LaTeX 格式（行内 $...$，独立 $$...$$）。
2. 若公式不完整，尝试基于上下文补全，并在 confidence 中降低分值。
3. 多个公式请用 \\\\ 分隔。
"""
        else:
            instruction = """
分析要求：
1. 文字按行转录，保留原文顺序。
2. 如有草图或箭头关系，用文字描述在 recognized_text 中。
3. 对模糊区域明确标注 [?]。
"""

        full_prompt = f"""
{system_role}
{instruction}
当前课程背景：{context if context else "无"}

请严格按照以下 JSON Schema 返回，不要添加任何其他内容：
{schema_example}

手写图片如下：
"""
        return full_prompt

    # ================= 原有动态 Prompt（保留用于非严格场景） =================
    def _build_dynamic_prompt(self, whiteboard_type: WhiteboardType, context: str) -> str:
        """原有的通用 Prompt（当 strict_format=False 时使用）"""
        system_role = "你是一位资深的大学教授，擅长解读板书、手写内容、公式与代码。"
        context_info = f"当前课程背景：{context}。\n" if context else ""

        if whiteboard_type == WhiteboardType.CODE:
            task = """
请仔细识别白板截图中手写或打印的代码片段（语言可能为 Java/C++/Python/JavaScript 等）。
任务要求：
1. 将识别出的代码使用标准格式排版，并用对应的语言标记代码块（```语言）。
2. 检查代码中是否存在语法错误、逻辑漏洞或潜在 Bug，逐一指出并给出修正方案。
3. 提供一条性能优化或代码可读性方面的建议。
4. 若代码不完整，请基于上下文合理推测并补充缺失部分。
5. 最终回答必须使用 **Markdown** 格式组织，层次清晰。
"""
        elif whiteboard_type == WhiteboardType.MATH:
            task = """
请识别白板截图中的数学公式、方程或推导过程。
任务要求：
1. 使用 LaTeX 语法呈现所有公式，行内公式用 $...$，独立公式用 $$...$$。
2. 给出完整的推导步骤或解题思路，解释每一步的依据。
3. 若包含图形或几何关系，用文字描述清楚。
4. 最终回答请严格使用 **Markdown** 格式，公式与文字间隔合理。
"""
        elif whiteboard_type == WhiteboardType.PHYSICS:
            task = """
请识别白板截图中的物理情景，包括但不限于公式、受力分析图、电路简图或实验装置草图。
任务要求：
1. 首先用文字描述物理场景和涉及的物理定律。
2. 列出关键公式（LaTeX 格式）。
3. 提供分步骤的解题或分析过程。
4. 如有简图，用语言描述其含义。
5. 回答请使用 **Markdown** 格式，条理清晰。
"""
        else:
            task = """
请识别白板截图中的所有内容（文字、公式、代码、草图等）。
任务要求：
1. 准确转录文字内容。
2. 公式部分使用 LaTeX 语法呈现。
3. 代码部分使用代码块标注语言类型。
4. 对模糊不清的内容请注明并建议用户重试。
5. 回答请使用 **Markdown** 格式。
"""

        quality_note = """
---
**重要提示**：
- 如果图片内容模糊、不完整或无法辨识，请如实告知用户具体原因，并提供清晰化的建议。
- 请勿编造不存在的内容，保持专业严谨。
"""
        return f"{system_role}\n{context_info}{task}\n{quality_note}"

    # ================= 新增：后处理校验 JSON =================
    def _extract_and_validate_json(self, raw_content: str) -> Tuple[bool, dict, str]:
        """
        从模型原始输出中提取 JSON，校验 schema，返回 (是否有效, json_dict, 错误信息)
        """
        # 1. 移除可能的 Markdown 代码块标记
        clean = re.sub(r'^```json\s*|\s*```$', '', raw_content.strip(), flags=re.MULTILINE)
        # 2. 尝试解析 JSON
        try:
            data = json.loads(clean)
        except json.JSONDecodeError:
            # 尝试匹配第一个 { 到最后一个 }
            match = re.search(r'\{.*\}', clean, re.DOTALL)
            if match:
                try:
                    data = json.loads(match.group(0))
                except Exception:
                    return False, {}, f"JSON 解析失败: {raw_content[:100]}..."
            else:
                return False, {}, "未找到有效的 JSON 对象"

        # 3. 校验并补全必需字段
        if "recognized_text" not in data:
            data["recognized_text"] = ""
        if "confidence" not in data:
            data["confidence"] = 0.5
        if "ambiguous_spots" not in data:
            data["ambiguous_spots"] = []
        if "structured_content" not in data:
            data["structured_content"] = {"type": "unknown", "body": data["recognized_text"]}
        if "type" not in data["structured_content"]:
            data["structured_content"]["type"] = "unknown"
        if "body" not in data["structured_content"]:
            data["structured_content"]["body"] = data["recognized_text"]

        return True, data, ""

    # ================= 原有重试机制（保持不变） =================
    def _call_api_with_retry(self, payload: Dict[str, Any]) -> Tuple[bool, str, str]:
        for attempt in range(self.MAX_RETRIES + 1):
            try:
                resp = self.session.post(
                    self.API_URL,
                    json=payload,
                    timeout=self.TIMEOUT
                )
                if resp.status_code == 200:
                    data = resp.json()
                    content = data.get("choices", [{}])[0].get("message", {}).get("content", "")
                    if content:
                        return True, content, ""
                    else:
                        error_msg = "AI 返回内容为空，请重试。"
                        logger.warning(f"第 {attempt+1} 次尝试: {error_msg}")
                        if attempt < self.MAX_RETRIES:
                            time.sleep(self.RETRY_DELAY * (attempt + 1))
                            continue
                        return False, "", error_msg
                else:
                    try:
                        error_data = resp.json()
                        error_msg = error_data.get("error", {}).get("message", f"HTTP {resp.status_code}")
                    except:
                        error_msg = f"HTTP {resp.status_code}: {resp.text[:200]}"
                    logger.warning(f"第 {attempt+1} 次请求失败: {error_msg}")
                    if resp.status_code in [429, 500, 502, 503, 504]:
                        if attempt < self.MAX_RETRIES:
                            wait_time = self.RETRY_DELAY * (2 ** attempt)
                            time.sleep(wait_time)
                            continue
                    return False, "", self._human_readable_error(resp.status_code, error_msg)
            except requests.exceptions.Timeout:
                error_msg = "请求超时，AI 服务响应较慢。"
                logger.warning(f"第 {attempt+1} 次超时")
                if attempt < self.MAX_RETRIES:
                    time.sleep(self.RETRY_DELAY * (attempt + 1))
                    continue
                return False, "", error_msg
            except requests.exceptions.ConnectionError as e:
                error_msg = "网络连接异常，请检查网络后重试。"
                logger.error(f"连接错误: {e}")
                if attempt < self.MAX_RETRIES:
                    time.sleep(self.RETRY_DELAY * 2)
                    continue
                return False, "", error_msg
            except Exception as e:
                error_msg = f"未知错误: {str(e)}"
                logger.exception("未预期的异常")
                return False, "", "系统内部错误，请联系管理员。"
        return False, "", "多次重试后仍失败，请稍后再试。"

    def _human_readable_error(self, status_code: int, original_msg: str) -> str:
        if status_code == 401:
            return "AI 服务授权失败，请联系系统管理员检查 API Key。"
        elif status_code == 403:
            return "AI 服务访问被拒绝，可能是权限不足或配额用尽。"
        elif status_code == 429:
            return "AI 服务当前请求过于频繁，请稍后重试。"
        elif status_code == 400:
            return f"请求参数有误，请检查图片大小或格式。 ({original_msg})"
        elif status_code >= 500:
            return "AI 服务暂时不可用，请稍后再试。"
        else:
            return f"AI 服务调用失败：{original_msg}"


# ================= 便捷函数（供外部调用） =================
def analyze_whiteboard_image(image_path: str = None,
                             image_base64: str = None,
                             whiteboard_type: str = "general",
                             api_key: str = None,
                             context: str = "",
                             strict_format: bool = True) -> AIResponse:
    """
    便捷函数，支持文件路径或 Base64 输入
    """
    if not api_key:
        import os
        api_key = os.environ.get("ZHIPU_API_KEY")
        if not api_key:
            raise ValueError("请设置环境变量 ZHIPU_API_KEY 或传入 api_key 参数")

    if image_path:
        with open(image_path, "rb") as f:
            b64_data = base64.b64encode(f.read()).decode()
    elif image_base64:
        b64_data = image_base64
    else:
        raise ValueError("必须提供 image_path 或 image_base64 之一")

    type_map = {
        "code": WhiteboardType.CODE,
        "math": WhiteboardType.MATH,
        "physics": WhiteboardType.PHYSICS,
        "general": WhiteboardType.GENERAL
    }
    wb_type = type_map.get(whiteboard_type.lower(), WhiteboardType.GENERAL)

    analyzer = ZhipuVisionAnalyzer(api_key)
    return analyzer.analyze_image(b64_data, whiteboard_type=wb_type, context=context, strict_format=strict_format)


# ================= 使用示例 =================
if __name__ == "__main__":
    # 测试代码
    import os
    API_KEY = os.environ.get("ZHIPU_API_KEY", "你的真实API_KEY")
    analyzer = ZhipuVisionAnalyzer(API_KEY)

    # 读取一张测试图片
    test_image = "test.jpg"
    if os.path.exists(test_image):
        with open(test_image, "rb") as f:
            img_b64 = base64.b64encode(f.read()).decode()
        # 严格模式测试（手写连笔字推荐）
        resp = analyzer.analyze_image(img_b64, whiteboard_type=WhiteboardType.MATH,
                                      context="高等数学", strict_format=True)
        if resp.success:
            print("识别成功，返回 JSON：")
            print(resp.content)
        else:
            print("失败:", resp.error_msg)
    else:
        print(f"请准备测试图片 {test_image}")