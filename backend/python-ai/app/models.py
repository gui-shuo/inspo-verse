"""Pydantic models for the AI service."""

from typing import Literal
from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    role: Literal["user", "assistant", "system"]
    content: str = Field(min_length=1)


class ChatRequest(BaseModel):
    sessionId: str = Field(min_length=1)
    model: str = Field(default="creative")
    messages: list[ChatMessage] = Field(min_length=1)


class VoiceTranscriptionResponse(BaseModel):
    text: str
    duration_ms: int | None = None


class FileExtractionResponse(BaseModel):
    text: str
    file_type: str
    file_name: str
    char_count: int
