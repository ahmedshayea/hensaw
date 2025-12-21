"""Configuration utilities for gateway service.

This module provides utility functions for accessing application settings,
including a cached function to ensure settings are loaded only once.
"""

from functools import lru_cache

from gateway.core.settings import Settings


@lru_cache
def get_settings() -> Settings:
    """Get application settings with caching.

    This function uses functools.lru_cache to ensure settings are loaded
    only once and cached for subsequent calls, improving performance.

    Returns:
        Settings: The application settings instance.

    Example:
        >>> from gateway.core.config import get_settings
        >>> settings = get_settings()
        >>> print(settings.app_name)
        Gateway Service
    """
    return Settings()
