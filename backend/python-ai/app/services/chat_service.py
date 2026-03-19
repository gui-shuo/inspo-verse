"""Chat service using LangChain + DashScope (阿里云通义千问)."""

import json
import logging
from collections.abc import AsyncGenerator

from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, AIMessage, SystemMessage

from app.config import (
    DASHSCOPE_API_KEY,
    DASHSCOPE_BASE_URL,
    MODEL_MAP,
    DEFAULT_MODEL,
    SYSTEM_PROMPTS,
    DEFAULT_SYSTEM_PROMPT,
    MAX_CONTEXT_MESSAGES,
)
from app.models import ChatMessage

logger = logging.getLogger(__name__)


def _get_model_name(mode: str) -> str:
    """Map the frontend mode id to a real DashScope model name."""
    return MODEL_MAP.get(mode, DEFAULT_MODEL)


def _get_system_prompt(mode: str) -> str:
    """Return the system prompt for the given mode."""
    return SYSTEM_PROMPTS.get(mode, DEFAULT_SYSTEM_PROMPT)


def _build_langchain_messages(
    mode: str, messages: list[ChatMessage]
) -> list[SystemMessage | HumanMessage | AIMessage]:
    """Convert API messages to LangChain message objects, prepend system prompt."""
    lc_messages: list[SystemMessage | HumanMessage | AIMessage] = [
        SystemMessage(content=_get_system_prompt(mode))
    ]

    # Only keep the last N messages for context window management
    recent = messages[-MAX_CONTEXT_MESSAGES:]

    for m in recent:
        if m.role == "system":
            lc_messages.append(SystemMessage(content=m.content))
        elif m.role == "user":
            lc_messages.append(HumanMessage(content=m.content))
        elif m.role == "assistant":
            lc_messages.append(AIMessage(content=m.content))

    return lc_messages


def _sse_event(event: str, payload: dict) -> str:
    """Format a Server-Sent Event."""
    return f"event: {event}\ndata: {json.dumps(payload, ensure_ascii=False)}\n\n"


async def stream_chat(
    session_id: str,
    mode: str,
    messages: list[ChatMessage],
) -> AsyncGenerator[str, None]:
    """
    Stream a chat completion from DashScope via LangChain.

    Yields SSE events:
      - event: start   → {"status": "ok", "model": "..."}
      - event: token   → {"delta": "...", "tokenIndex": N}
      - event: end     → {"finishReason": "stop", "totalTokens": N}
      - event: error   → {"message": "..."}
    """
    model_name = _get_model_name(mode)

    yield _sse_event("start", {"status": "ok", "model": model_name})

    try:
        llm = ChatOpenAI(
            model=model_name,
            openai_api_key=DASHSCOPE_API_KEY,
            openai_api_base=DASHSCOPE_BASE_URL,
            streaming=True,
            temperature=0.8 if mode == "creative" else 0.3,
            max_tokens=4096,
        )

        lc_messages = _build_langchain_messages(mode, messages)
        full_content = ""
        token_index = 0

        async for chunk in llm.astream(lc_messages):
            delta = chunk.content
            if delta:
                full_content += delta
                token_index += 1
                yield _sse_event("token", {
                    "delta": delta,
                    "tokenIndex": token_index,
                })

        yield _sse_event("end", {
            "finishReason": "stop",
            "totalTokens": token_index,
            "fullContent": full_content,
        })

    except Exception as e:
        logger.exception("Chat streaming error for session %s", session_id)
        yield _sse_event("error", {"message": f"AI 服务异常: {str(e)}"})
