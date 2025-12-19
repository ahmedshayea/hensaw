"""Example application module to demonstrate the project scaffold."""

from fastapi import FastAPI


def create_app() -> FastAPI:
    """Build a minimal FastAPI application for local development."""
    app = FastAPI(title="Gateway Service", version="0.1.0")

    @app.get("/healthz", tags=["health"])
    def healthcheck() -> dict[str, str]:
        return {"status": "ok"}

    return app
