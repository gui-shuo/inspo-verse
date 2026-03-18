import asyncio
import json
import os
from typing import AsyncGenerator, Literal

from fastapi import FastAPI, Header, HTTPException
from fastapi.responses import StreamingResponse, JSONResponse
from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    role: Literal["user", "assistant", "system"]
    content: str = Field(min_length=1)


class ChatRequest(BaseModel):
    sessionId: str = Field(min_length=1)
    model: str = Field(default="creative")
    messages: list[ChatMessage] = Field(min_length=1)


app = FastAPI(title="inspo-python-ai", version="0.1.0")


def _verify_internal_sign(sign: str | None) -> None:
    expected = os.getenv("AI_INTERNAL_SIGN_KEY")
    if expected and not sign:
        raise HTTPException(status_code=401, detail="Missing x-internal-sign header required for internal service authentication")
    if expected and sign != expected:
        raise HTTPException(status_code=401, detail="Authentication failed for internal service call")


def _response_event(event: str, payload: dict) -> str:
    return f"event: {event}\ndata: {json.dumps(payload, ensure_ascii=False)}\n\n"


async def _mock_stream(content: str) -> AsyncGenerator[str, None]:
    yield _response_event("start", {"status": "ok"})
    answer = f"已收到你的请求：{content}。这是来自 Python AI 服务的流式响应示例。"
    for token in answer:
        await asyncio.sleep(0.02)
        yield _response_event("token", {"delta": token})
    yield _response_event("end", {"finishReason": "stop"})


@app.get("/healthz")
async def healthz() -> JSONResponse:
    return JSONResponse({"status": "ok"})


@app.post("/ai-stream/v1/chat/completions")
async def chat_completions(req: ChatRequest, x_internal_sign: str | None = Header(default=None)) -> StreamingResponse:
    _verify_internal_sign(x_internal_sign)
    last_user_message = next((m.content for m in reversed(req.messages) if m.role == "user"), "你好")
    return StreamingResponse(_mock_stream(last_user_message), media_type="text/event-stream")
