# Hensaw Vector Database System

Three services combine into a mini vector database stack:

- **Engine** – Java 21 gRPC server with a custom HNSW implementation. Lives in `services/engine` and exposes `Upsert` / `Query` RPCs.
- **Gateway** – FastAPI service (`services/gateway`) that embeds text with Sentence Transformers and forwards gRPC calls to the engine.
- **Web** – Next.js dashboard (`services/web`) that lets you upsert/query vectors through the gateway.

## Requirements

- Docker / Docker Compose (for container workflow)
- Java 21 toolchain + Gradle wrapper (engine)
- [uv](https://docs.astral.sh/uv/) + Python 3.12 (gateway)
- Bun ≥ 1.1 (frontend) and Node 20 for builds
- `protoc` plus `grpcio-tools` (`pip install grpcio-tools` or `uv tool install grpcio-tools`) to regenerate Python stubs

## Local Development

### Engine

```bash
cd services/engine
./gradlew installDist
./build/install/engine/bin/engine
```

### Gateway

```bash
cd services/gateway
uv sync
./scripts/generate_protos.sh   # requires grpcio-tools to be installed
uv run uvicorn gateway.app:create_app --host 0.0.0.0 --port 8000
```

### Web

```bash
cd services/web
bun install
NEXT_PUBLIC_API_URL=http://localhost:8000 bun run dev
```

## Running Everything via Docker Compose

```bash
docker compose up --build
```

This brings up:

| Service | Port | Notes |
| --- | --- | --- |
| `engine` | 50051 | Pure gRPC; no persistent volume by default |
| `gateway` | 8000 | Downloads the `all-MiniLM-L6-v2` model during build |
| `web` | 3000 | Talks to the gateway through `NEXT_PUBLIC_API_URL=http://gateway:8000` |

When using Compose you can inspect logs via `docker compose logs -f gateway` (or `engine`, `web`).

## Environment Variables

| Variable | Default | Scope | Description |
| --- | --- | --- | --- |
| `ENGINE_GRPC_HOST` | `engine` | Gateway | Hostname for the Java engine |
| `ENGINE_GRPC_PORT` | `50051` | Gateway | gRPC port to talk to the engine |
| `EMBEDDER_MODEL_NAME` | `all-MiniLM-L6-v2` | Gateway | Sentence Transformers model id |
| `NEXT_PUBLIC_API_URL` | `http://localhost:8000` | Web | Base URL for the FastAPI gateway |

## Regenerating gRPC Stubs

```bash
cd services/gateway
./scripts/generate_protos.sh
```

The script uses `grpc_tools.protoc` and writes `*_pb2.py` / `*_pb2_grpc.py` files into `src/gateway/gen`. If you only have `protoc` available you can still generate the message classes with:

```bash
protoc -I ../../proto --python_out=src/gateway/gen ../../proto/vector_service.proto
```

but the gRPC service bindings require the Python plugin from `grpcio-tools`.
