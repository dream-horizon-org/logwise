# Vector.dev log endpoint using direct binary installation
FROM debian:12-slim

WORKDIR /app

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    ca-certificates \
    protobuf-compiler \
    && rm -rf /var/lib/apt/lists/*

# Download and install Vector binary directly from GitHub releases
RUN ARCH=$(uname -m) && \
    if [ "$ARCH" = "x86_64" ]; then ARCH="x86_64"; elif [ "$ARCH" = "aarch64" ] || [ "$ARCH" = "arm64" ]; then ARCH="aarch64"; fi && \
    curl -L "https://github.com/vectordotdev/vector/releases/download/v0.45.0/vector-0.45.0-${ARCH}-unknown-linux-musl.tar.gz" \
    -o vector.tar.gz && \
    tar -xzf vector.tar.gz && \
    mv vector-*/bin/vector /usr/local/bin/vector && \
    chmod +x /usr/local/bin/vector && \
    rm -rf vector.tar.gz vector-*

# Create logs directory with proper permissions
RUN mkdir -p /app/logs && chmod 755 /app/logs

# Copy Vector configuration and protobuf source
COPY vector.toml /etc/vector/vector.toml
COPY logwise-vector.proto /app/logwise-vector.proto

# Generate protobuf descriptor file
RUN protoc --proto_path=/app --descriptor_set_out=/etc/vector/logwise-vector.desc --include_imports logwise-vector.proto

# Expose port for log ingestion and API
EXPOSE 4318 8686

# Run Vector with the configuration
CMD ["vector", "--config", "/etc/vector/vector.toml"]
