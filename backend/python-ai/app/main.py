"""
Inspo-Verse Python AI Service
─────────────────────────────
Provides:
  - LLM chat completions via LangChain + DashScope (阿里云通义千问)
  - Voice-to-text transcription via DashScope Paraformer ASR
  - File content extraction (PDF / TXT / DOCX / code files)
"""

import logging

from fastapi import FastAPI, Header, HTTPException, UploadFile, File
from fastapi.responses import StreamingResponse, JSONResponse

from app.config import AI_INTERNAL_SIGN_KEY, MAX_FILE_SIZE_MB, MAX_AUDIO_SIZE_MB
from app.models import ChatRequest, VoiceTranscriptionResponse, FileExtractionResponse
from app.services.chat_service import stream_chat
from app.services.voice_service import transcribe_audio
from app.services.file_service import extract_file_content

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="inspo-python-ai", version="1.0.0")


# ── Internal auth ────────────────────────────────────────────────
def _verify_internal_sign(sign: str | None) -> None:
    if AI_INTERNAL_SIGN_KEY and not sign:
        raise HTTPException(
            status_code=401,
            detail="Missing x-internal-sign header",
        )
    if AI_INTERNAL_SIGN_KEY and sign != AI_INTERNAL_SIGN_KEY:
        raise HTTPException(
            status_code=401,
            detail="Invalid x-internal-sign",
        )


# ── Health check ─────────────────────────────────────────────────
@app.get("/healthz")
async def healthz() -> JSONResponse:
    return JSONResponse({"status": "ok"})


# ── Chat completions (SSE streaming) ─────────────────────────────
@app.post("/ai-stream/v1/chat/completions")
async def chat_completions(
    req: ChatRequest,
    x_internal_sign: str | None = Header(default=None),
) -> StreamingResponse:
    """Stream LLM chat completion via SSE."""
    _verify_internal_sign(x_internal_sign)

    return StreamingResponse(
        stream_chat(
            session_id=req.sessionId,
            mode=req.model,
            messages=req.messages,
        ),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )


# ── Voice transcription ─────────────────────────────────────────
@app.post("/ai-stream/v1/voice/transcribe")
async def voice_transcribe(
    file: UploadFile = File(...),
    x_internal_sign: str | None = Header(default=None),
) -> JSONResponse:
    """Transcribe uploaded audio to text via DashScope Paraformer ASR."""
    _verify_internal_sign(x_internal_sign)

    # Validate file size
    file_bytes = await file.read()
    size_mb = len(file_bytes) / (1024 * 1024)
    if size_mb > MAX_AUDIO_SIZE_MB:
        raise HTTPException(
            status_code=400,
            detail=f"音频文件过大: {size_mb:.1f}MB，最大 {MAX_AUDIO_SIZE_MB}MB",
        )

    try:
        result = await transcribe_audio(file_bytes, file.filename or "audio.wav")
        return JSONResponse({"code": 0, "data": result})
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.exception("Voice transcription error")
        raise HTTPException(status_code=500, detail=f"语音识别失败: {str(e)}")


# ── File content extraction ──────────────────────────────────────
@app.post("/ai-stream/v1/file/extract")
async def file_extract(
    file: UploadFile = File(...),
    x_internal_sign: str | None = Header(default=None),
) -> JSONResponse:
    """Extract text content from uploaded file (PDF, TXT, DOCX, code)."""
    _verify_internal_sign(x_internal_sign)

    file_bytes = await file.read()
    size_mb = len(file_bytes) / (1024 * 1024)
    if size_mb > MAX_FILE_SIZE_MB:
        raise HTTPException(
            status_code=400,
            detail=f"文件过大: {size_mb:.1f}MB，最大 {MAX_FILE_SIZE_MB}MB",
        )

    try:
        result = await extract_file_content(file_bytes, file.filename or "file.txt")
        return JSONResponse({"code": 0, "data": result})
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.exception("File extraction error")
        raise HTTPException(status_code=500, detail=f"文件解析失败: {str(e)}")
