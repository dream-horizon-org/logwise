FROM grafana/grafana:latest

# Use a plugins dir that won't be masked by the data volume
ENV GF_PATHS_PLUGINS=/opt/grafana-plugins

USER root

# Create plugins directory with proper permissions
RUN mkdir -p "$GF_PATHS_PLUGINS" && \
    chown -R 472:472 "$GF_PATHS_PLUGINS"

# Install plugins directly to custom location using --pluginsDir
# This ensures plugins are installed in the correct location from the start
RUN grafana-cli --pluginsDir "$GF_PATHS_PLUGINS" plugins install grafana-athena-datasource && \
    grafana-cli --pluginsDir "$GF_PATHS_PLUGINS" plugins install yesoreyeram-infinity-datasource

# Ensure proper ownership and permissions
RUN chown -R 472:472 "$GF_PATHS_PLUGINS" && \
    find "$GF_PATHS_PLUGINS" -type d -exec chmod 755 {} \; && \
    find "$GF_PATHS_PLUGINS" -type f -exec chmod 644 {} \; && \
    find "$GF_PATHS_PLUGINS" -name "*.sh" -exec chmod 755 {} \; && \
    find "$GF_PATHS_PLUGINS" -name "gpx_*" -exec chmod 755 {} \;

# Copy datasource provisioning script (curl should be available in Grafana image)
COPY provision-datasources.sh /usr/local/bin/provision-datasources.sh
RUN chmod +x /usr/local/bin/provision-datasources.sh && \
    chown 472:472 /usr/local/bin/provision-datasources.sh

# Verify plugins are installed
RUN echo "=== Plugins installed ===" && \
    ls -la "$GF_PATHS_PLUGINS" && \
    echo "=== Plugin binaries ===" && \
    find "$GF_PATHS_PLUGINS" -name "gpx_*" -exec ls -lh {} \;

# Drop privileges
USER 472
