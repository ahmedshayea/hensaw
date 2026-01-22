"""Gateway FastAPI application."""

from __future__ import annotations

import logging

from fastapi import Depends, FastAPI, HTTPException
from fastapi.responses import JSONResponse
from grpc import RpcError, StatusCode

from .clients import EngineClient, get_engine_client
from .routers import router as vector_router
from .schemas import HealthResponse

LOGGER = logging.getLogger(__name__)


def create_app() -> FastAPI:
    """Construct the FastAPI application."""
    app = FastAPI(title="Gateway Service", version="0.1.0")

    @app.get("/healthz", response_model=HealthResponse)
    async def healthcheck(
        engine: EngineClient = Depends(get_engine_client),
    ) -> HealthResponse:
        try:
            engine.ping()
        except RpcError as exc:  # pragma: no cover - network failure path
            LOGGER.exception("Engine healthcheck failed")
            raise HTTPException(status_code=503, detail="Engine unavailable") from exc
        return HealthResponse(status="ok")

    app.include_router(vector_router)

    @app.exception_handler(RpcError)
    async def grpc_exception_handler(
        _: FastAPI, exc: RpcError
    ):  # pragma: no cover - handled by FastAPI
        status_map = {
            StatusCode.INVALID_ARGUMENT: 400,
            StatusCode.NOT_FOUND: 404,
            StatusCode.UNAVAILABLE: 503,
        }
        http_status = status_map.get(exc.code(), 500)
        return JSONResponse(status_code=http_status, content={"detail": exc.details()})

    return app
