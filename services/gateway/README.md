## Gateway Service

Scaffolded FastAPI project managed with [uv](https://docs.astral.sh/uv/).

### Configuration

The service uses Pydantic Settings for configuration management. Settings can be configured through environment variables or a `.env` file.

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

2. Update the `.env` file with your configuration values.

#### Using Settings in Code

```python
from gateway.core import get_settings

settings = get_settings()
print(settings.app_name)  # Gateway Service
print(settings.api_port)  # 8000
```

The `get_settings()` function uses caching to ensure settings are loaded only once.

### Development

```bash
uv sync
uv run fastapi dev src/gateway/app.py
```

### Testing

```bash
uv run pytest
```
