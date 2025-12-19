"""Gateway package root for the services.gateway project."""

from importlib.metadata import PackageNotFoundError, version


try:
    __version__ = version("gateway")
except PackageNotFoundError:  # pragma: no cover - fallback during local dev
    __version__ = "0.0.0"

__all__ = ["__version__"]
