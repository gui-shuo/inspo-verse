"""File content extraction service for PDF, TXT, DOCX."""

import logging
import os
from pathlib import Path

from app.config import UPLOAD_TMP_DIR, MAX_FILE_SIZE_MB

logger = logging.getLogger(__name__)

# Supported file types and their MIME types
SUPPORTED_FILE_TYPES = {
    ".txt": "text/plain",
    ".pdf": "application/pdf",
    ".docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    ".md": "text/markdown",
    ".csv": "text/csv",
    ".json": "application/json",
    ".py": "text/x-python",
    ".java": "text/x-java",
    ".js": "text/javascript",
    ".ts": "text/typescript",
    ".html": "text/html",
    ".css": "text/css",
    ".xml": "text/xml",
    ".yml": "text/yaml",
    ".yaml": "text/yaml",
}

# Max characters to extract from a file
MAX_EXTRACT_CHARS = 50000


def _extract_txt(file_path: str) -> str:
    """Extract text from a plain text file."""
    with open(file_path, "r", encoding="utf-8", errors="replace") as f:
        return f.read(MAX_EXTRACT_CHARS)


def _extract_pdf(file_path: str) -> str:
    """Extract text from a PDF file."""
    from PyPDF2 import PdfReader

    reader = PdfReader(file_path)
    text_parts = []
    total_chars = 0

    for page in reader.pages:
        page_text = page.extract_text() or ""
        text_parts.append(page_text)
        total_chars += len(page_text)
        if total_chars >= MAX_EXTRACT_CHARS:
            break

    return "\n".join(text_parts)[:MAX_EXTRACT_CHARS]


def _extract_docx(file_path: str) -> str:
    """Extract text from a DOCX file."""
    from docx import Document

    doc = Document(file_path)
    text_parts = []
    total_chars = 0

    for para in doc.paragraphs:
        text_parts.append(para.text)
        total_chars += len(para.text)
        if total_chars >= MAX_EXTRACT_CHARS:
            break

    return "\n".join(text_parts)[:MAX_EXTRACT_CHARS]


async def extract_file_content(file_bytes: bytes, filename: str) -> dict:
    """
    Extract text content from an uploaded file.

    Returns:
        {"text": "...", "file_type": "pdf", "file_name": "...", "char_count": N}
    """
    ext = Path(filename).suffix.lower()

    if ext not in SUPPORTED_FILE_TYPES:
        raise ValueError(
            f"不支持的文件格式: {ext}，支持: {', '.join(SUPPORTED_FILE_TYPES.keys())}"
        )

    # Check file size
    size_mb = len(file_bytes) / (1024 * 1024)
    if size_mb > MAX_FILE_SIZE_MB:
        raise ValueError(f"文件过大: {size_mb:.1f}MB，最大支持 {MAX_FILE_SIZE_MB}MB")

    # Save to temp file for extraction
    tmp_path = os.path.join(UPLOAD_TMP_DIR, f"file_{os.urandom(8).hex()}{ext}")
    try:
        with open(tmp_path, "wb") as f:
            f.write(file_bytes)

        # Extract based on file type
        if ext == ".pdf":
            text = _extract_pdf(tmp_path)
        elif ext == ".docx":
            text = _extract_docx(tmp_path)
        else:
            # All other supported types are text-based
            text = _extract_txt(tmp_path)

        return {
            "text": text,
            "file_type": ext.lstrip("."),
            "file_name": filename,
            "char_count": len(text),
        }

    finally:
        if os.path.exists(tmp_path):
            os.remove(tmp_path)
