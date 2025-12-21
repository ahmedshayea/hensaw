"""Settings module for gateway service configuration.

This module defines the application settings using Pydantic BaseSettings,
which allows loading configuration from environment variables and .env files.
"""

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings loaded from environment variables.

    All settings can be overridden using environment variables.
    For example, APP_NAME can be set via the APP_NAME environment variable.
    """

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # Application settings
    app_name: str = "Gateway Service"
    app_version: str = "0.1.0"
    debug: bool = False
    environment: str = "development"

    # API settings
    api_host: str = "0.0.0.0"
    api_port: int = 8000
    api_prefix: str = "/api/v1"

    # CORS settings
    cors_origins: list[str] = ["*"]
    cors_allow_credentials: bool = True
    cors_allow_methods: list[str] = ["*"]
    cors_allow_headers: list[str] = ["*"]

    # Database settings (example)
    database_url: str = "postgresql://user:password@localhost:5432/hensaw"
    database_pool_size: int = 10
    database_max_overflow: int = 20

    # Vector Engine gRPC settings
    engine_grpc_host: str = "localhost"
    engine_grpc_port: int = 50051

    # Embedding settings
    embedding_model: str = "text-embedding-ada-002"
    embedding_dimension: int = 1536

    # Security settings
    secret_key: str = "change-me-in-production"
    api_key: str | None = None

    # Logging settings
    log_level: str = "INFO"
    log_format: str = "json"

    # Rate limiting
    rate_limit_per_minute: int = 60
