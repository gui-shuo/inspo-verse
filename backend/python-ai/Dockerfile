# ============================================
# Inspo-Verse Python AI Service
# ============================================

FROM python:3.11-slim

LABEL maintainer="Inspo-Verse Team"

# 防止 Python 缓冲输出、不生成 .pyc
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# 安装依赖（利用 Docker 缓存）
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY app ./app

# 创建非 root 用户
RUN groupadd -r app && useradd -r -g app app \
    && mkdir -p /tmp/inspo-ai-uploads \
    && chown -R app:app /app /tmp/inspo-ai-uploads

USER app

EXPOSE 9000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "9000", "--workers", "2"]
