## Web Dashboard

Next.js 16 front-end for interacting with the gateway. Built with Bun + shadcn UI.

### Local Development

```bash
bun install
NEXT_PUBLIC_API_URL=http://localhost:8000 bun run dev
```

### Lint & Build

```bash
bun run lint
bun run build
```

### Docker

The service ships with a multi-stage Dockerfile. Build with:

```bash
docker build -t hensaw-web .
```
