"""gRPC client helpers for communicating with the vector engine."""

from __future__ import annotations

import functools
import importlib
import os
from typing import Any

import grpc


def _load_pb_modules():
    try:
        pb2 = importlib.import_module("gateway.gen.vector_service_pb2")
        pb2_grpc = importlib.import_module("gateway.gen.vector_service_pb2_grpc")
    except ModuleNotFoundError as exc:  # pragma: no cover
        raise RuntimeError(
            "Protocol buffer stubs not found. Regenerate them via grpc_tools.protoc."
        ) from exc
    return pb2, pb2_grpc


class EngineClient:
    """Thin wrapper around the generated gRPC stub."""

    def __init__(self, host: str, port: int) -> None:
        self._target = f"{host}:{port}"
        pb2, pb2_grpc = _load_pb_modules()
        self._pb2 = pb2
        self._channel = grpc.insecure_channel(self._target)
        self._stub = pb2_grpc.VectorServiceStub(self._channel)

    def close(self) -> None:
        self._channel.close()

    def ping(self) -> None:
        grpc.channel_ready_future(self._channel).result(timeout=2)

    def build_vector(
        self, *, vector_id: str, values: list[float], metadata: dict[str, str]
    ) -> Any:
        return self._pb2.Vector(id=vector_id, values=values, metadata=metadata)

    def upsert(self, namespace: str, vectors: list[Any]) -> Any:
        request = self._pb2.UpsertRequest(namespace=namespace, vectors=vectors)
        return self._stub.Upsert(request)

    def query(
        self,
        namespace: str,
        vector: list[float],
        top_k: int,
        include_values: bool,
        include_metadata: bool,
    ) -> Any:
        request = self._pb2.QueryRequest(
            namespace=namespace,
            vector=vector,
            top_k=top_k,
            include_values=include_values,
            include_metadata=include_metadata,
        )
        return self._stub.Query(request)


@functools.lru_cache(maxsize=1)
def get_engine_client() -> EngineClient:
    host = os.getenv("ENGINE_GRPC_HOST", "engine")
    port = int(os.getenv("ENGINE_GRPC_PORT", "50051"))
    return EngineClient(host, port)
