# Gateway Service

FastAPI gateway that embeds text with Sentence Transformers and proxies requests to the Java engine over gRPC. Managed with [uv](https://docs.astral.sh/uv/).

## Development

```bash
# Install dependencies
uv sync

# Generate protobuf stubs (requires grpcio-tools)
./scripts/generate_protos.sh

# Run the API locally
uv run uvicorn gateway.app:create_app --host 0.0.0.0 --port 8000
```

Environment variables:

| Variable | Default | Description |
| --- | --- | --- |
| `ENGINE_GRPC_HOST` | `engine` | Hostname for the Java engine |
| `ENGINE_GRPC_PORT` | `50051` | gRPC port |
| `EMBEDDER_MODEL_NAME` | `all-MiniLM-L6-v2` | Sentence Transformer model |
