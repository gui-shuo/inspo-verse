"""Voice transcription service using DashScope Paraformer ASR."""

import logging
import os
import tempfile
from pathlib import Path

import dashscope
from dashscope.audio.asr import Transcription

from app.config import DASHSCOPE_API_KEY, UPLOAD_TMP_DIR

logger = logging.getLogger(__name__)

# Supported audio formats
SUPPORTED_AUDIO_FORMATS = {".wav", ".mp3", ".m4a", ".flac", ".ogg", ".webm", ".amr"}


async def transcribe_audio(file_bytes: bytes, filename: str) -> dict:
    """
    Transcribe audio to text using DashScope Paraformer.

    Returns:
        {"text": "transcribed text", "duration_ms": duration}
    """
    ext = Path(filename).suffix.lower()
    if ext not in SUPPORTED_AUDIO_FORMATS:
        raise ValueError(
            f"不支持的音频格式: {ext}，支持: {', '.join(SUPPORTED_AUDIO_FORMATS)}"
        )

    # Save to temp file (DashScope needs a file path or URL)
    tmp_path = os.path.join(UPLOAD_TMP_DIR, f"voice_{os.urandom(8).hex()}{ext}")
    try:
        with open(tmp_path, "wb") as f:
            f.write(file_bytes)

        dashscope.api_key = DASHSCOPE_API_KEY

        # Use Paraformer real-time recognition
        from dashscope.audio.asr import Recognition

        recognition = Recognition(
            model="paraformer-realtime-v2",
            format=ext.lstrip("."),
            sample_rate=16000,
            callback=None,
        )

        result = recognition.call(tmp_path)

        if result.status_code == 200:
            # Extract text from result
            text = ""
            if hasattr(result, "output") and result.output:
                if "text" in result.output:
                    text = result.output["text"]
                elif "sentence" in result.output:
                    sentences = result.output["sentence"]
                    text = "".join(s.get("text", "") for s in sentences)

            if not text and hasattr(result, "output"):
                # Try alternative result structure
                output = result.output
                if isinstance(output, dict):
                    for key in ("text", "result", "transcription"):
                        if key in output:
                            text = str(output[key])
                            break

            return {
                "text": text or "（未识别到语音内容）",
                "duration_ms": None,
            }
        else:
            logger.error(
                "ASR failed: status=%s, message=%s",
                result.status_code,
                getattr(result, "message", "unknown"),
            )
            raise RuntimeError(
                f"语音识别失败: {getattr(result, 'message', '未知错误')}"
            )

    finally:
        # Cleanup temp file
        if os.path.exists(tmp_path):
            os.remove(tmp_path)
