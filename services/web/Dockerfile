FROM oven/bun:latest

WORKDIR /app

# Copy package files
COPY package.json bun.lockb ./

# Install dependencies
RUN bun install

# Copy source
COPY . .

# Expose HTTP port
EXPOSE 3000

# Run application
CMD ["bun", "run", "dev"]
