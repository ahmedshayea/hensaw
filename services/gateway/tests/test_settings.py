"""Tests for core settings and configuration."""

import os
from unittest.mock import patch

from gateway.core.config import get_settings
from gateway.core.settings import Settings


def test_settings_default_values():
    """Test that settings have correct default values."""
    settings = Settings()

    assert settings.app_name == "Gateway Service"
    assert settings.app_version == "0.1.0"
    assert settings.debug is False
    assert settings.environment == "development"
    assert settings.api_host == "0.0.0.0"
    assert settings.api_port == 8000
    assert settings.api_prefix == "/api/v1"


def test_settings_from_environment():
    """Test that settings can be loaded from environment variables."""
    with patch.dict(
        os.environ,
        {
            "APP_NAME": "Test Gateway",
            "DEBUG": "true",
            "API_PORT": "9000",
            "ENVIRONMENT": "testing",
        },
    ):
        settings = Settings()
        assert settings.app_name == "Test Gateway"
        assert settings.debug is True
        assert settings.api_port == 9000
        assert settings.environment == "testing"


def test_get_settings_returns_settings_instance():
    """Test that get_settings returns a Settings instance."""
    settings = get_settings()
    assert isinstance(settings, Settings)


def test_get_settings_is_cached():
    """Test that get_settings returns the same instance when called multiple times."""
    # Clear the cache first
    get_settings.cache_clear()

    settings1 = get_settings()
    settings2 = get_settings()

    # Should be the same instance due to caching
    assert settings1 is settings2


def test_settings_grpc_configuration():
    """Test gRPC engine settings."""
    settings = Settings()

    assert settings.engine_grpc_host == "localhost"
    assert settings.engine_grpc_port == 50051


def test_settings_embedding_configuration():
    """Test embedding settings."""
    settings = Settings()

    assert settings.embedding_model == "text-embedding-ada-002"
    assert settings.embedding_dimension == 1536


def test_settings_cors_configuration():
    """Test CORS settings."""
    settings = Settings()

    assert settings.cors_origins == ["*"]
    assert settings.cors_allow_credentials is True
    assert settings.cors_allow_methods == ["*"]
    assert settings.cors_allow_headers == ["*"]
