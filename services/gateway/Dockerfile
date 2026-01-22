FROM python:3.12-slim AS base

ENV PIP_NO_CACHE_DIR=off \
    PIP_DISABLE_PIP_VERSION_CHECK=on \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN apt-get update && apt-get install -y --no-install-recommends build-essential git && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN pip install uv

COPY pyproject.toml uv.lock ./

RUN uv sync --locked --frozen --no-install-project

COPY . .

RUN uv run python -m grpc_tools.protoc \
    -I ../proto \
    --python_out=src/gateway/gen \
    --grpc_python_out=src/gateway/gen \
    ../proto/vector_service.proto

RUN uv run python - <<'PY'
from sentence_transformers import SentenceTransformer
SentenceTransformer("all-MiniLM-L6-v2")
PY

EXPOSE 8000

ENV ENGINE_GRPC_HOST=engine \
    ENGINE_GRPC_PORT=50051 \
    EMBEDDER_MODEL_NAME=all-MiniLM-L6-v2

CMD ["uv", "run", "uvicorn", "gateway.app:create_app", "--host", "0.0.0.0", "--port", "8000"]
