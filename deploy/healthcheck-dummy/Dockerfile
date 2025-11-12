FROM alpine:3.19

# Install OTEL collector and required packages
# Use multi-stage build to copy from official image
RUN apk add --no-cache \
    bash \
    su-exec \
    wget \
    ca-certificates

# Copy OTEL collector from official image
COPY --from=otel/opentelemetry-collector-contrib:0.100.0 \
    /otelcol-contrib /otelcol

# Create otelcol user (UID 10001, GID 10001) to match the official image
RUN addgroup -g 10001 otelcol && \
    adduser -D -u 10001 -G otelcol otelcol

# Create directories
RUN mkdir -p /var/log/healthcheck /var/lib/otelcol/storage && \
    chown -R otelcol:otelcol /var/log/healthcheck /var/lib/otelcol/storage

# Copy the healthcheck log generator script
COPY generate-healthcheck-logs.sh /usr/local/bin/generate-healthcheck-logs.sh
RUN chmod +x /usr/local/bin/generate-healthcheck-logs.sh

# Copy OTEL collector config
COPY otel-collector-config.yaml /etc/otelcol/config.yaml
RUN chown otelcol:otelcol /etc/otelcol/config.yaml

# Copy and set permissions for entrypoint
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Keep as root for entrypoint (will handle permissions for volumes)
# The collector will run as otelcol user
ENTRYPOINT ["/entrypoint.sh"]

