#!/usr/bin/env bash
set -euo pipefail

# Generate Python protobuf + gRPC stubs for the gateway service.
# Uses the workspace root as reference so the script can run from anywhere.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
PROTO_DIR="$ROOT_DIR/proto"
OUT_DIR="$ROOT_DIR/services/gateway/src/gateway/gen"

PROTOC_BIN=${GRPC_PYTHON_PROTOC:-grpc_tools.protoc}

if ! command -v "$PROTOC_BIN" >/dev/null 2>&1; then
  if command -v python-grpc-tools-protoc >/dev/null 2>&1; then
    PROTOC_BIN=python-grpc-tools-protoc
  else
    echo "grpc_tools.protoc not found. Install grpcio-tools (e.g. 'uv tool install grpcio-tools')." >&2
    exit 1
  fi
fi

"$PROTOC_BIN" \
  -I "$PROTO_DIR" \
  --python_out="$OUT_DIR" \
  --grpc_python_out="$OUT_DIR" \
  "$PROTO_DIR/vector_service.proto"
