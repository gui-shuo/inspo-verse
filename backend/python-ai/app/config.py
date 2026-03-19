"""Configuration for the Python AI service."""

import os


# ── DashScope (阿里云通义千问) ────────────────────────────────────
DASHSCOPE_API_KEY = os.getenv("DASHSCOPE_API_KEY", "")
DASHSCOPE_BASE_URL = os.getenv(
    "DASHSCOPE_BASE_URL",
    "https://dashscope.aliyuncs.com/compatible-mode/v1",
)

# ── Model mapping per scene/mode ─────────────────────────────────
# creative → 最强创意写作模型
# precise  → 最强精确问答模型
# coding   → 最强代码编程模型
MODEL_MAP: dict[str, str] = {
    "creative": os.getenv("MODEL_CREATIVE", "qwen-max"),
    "precise": os.getenv("MODEL_PRECISE", "qwen-plus"),
    "coding": os.getenv("MODEL_CODING", "qwen-coder-plus"),
}

DEFAULT_MODEL = "qwen-plus"

# ── Internal service authentication ──────────────────────────────
AI_INTERNAL_SIGN_KEY = os.getenv("AI_INTERNAL_SIGN_KEY", "")

# ── Limits ───────────────────────────────────────────────────────
MAX_CONTEXT_MESSAGES = int(os.getenv("MAX_CONTEXT_MESSAGES", "20"))
MAX_FILE_SIZE_MB = int(os.getenv("MAX_FILE_SIZE_MB", "10"))
MAX_AUDIO_SIZE_MB = int(os.getenv("MAX_AUDIO_SIZE_MB", "25"))

# ── System prompts per mode ──────────────────────────────────────
SYSTEM_PROMPTS: dict[str, str] = {
    "creative": (
        "你是 Inspo-Verse 的「灵感创作 V4」AI 助手。你擅长发散思维、创意写作、"
        "头脑风暴和艺术灵感。回答风格活泼有创意，善于使用比喻和故事化表达。"
        "使用 Markdown 格式美化输出。"
    ),
    "precise": (
        "你是 Inspo-Verse 的「精确问答 Pro」AI 助手。你基于事实、逻辑严密，"
        "回答准确可靠。优先引用可验证的信息，避免臆测。对不确定的内容会明确说明。"
        "使用 Markdown 格式美化输出。"
    ),
    "coding": (
        "你是 Inspo-Verse 的「代码编程 Max」AI 助手。你是一位资深全栈工程师，"
        "精通各种编程语言和框架。擅长代码生成、调试、重构和架构设计。"
        "代码示例使用正确的语法高亮 Markdown 代码块。"
    ),
}

DEFAULT_SYSTEM_PROMPT = SYSTEM_PROMPTS["creative"]

# ── Temp directory for uploads ───────────────────────────────────
UPLOAD_TMP_DIR = os.getenv("UPLOAD_TMP_DIR", "/tmp/inspo-ai-uploads")
os.makedirs(UPLOAD_TMP_DIR, exist_ok=True)
