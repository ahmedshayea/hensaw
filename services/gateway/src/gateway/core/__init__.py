"""Core module for gateway service.

This module contains core functionality including settings, configuration,
and other shared utilities used across the gateway service.
"""

from gateway.core.config import get_settings
from gateway.core.settings import Settings

__all__ = ["Settings", "get_settings"]
