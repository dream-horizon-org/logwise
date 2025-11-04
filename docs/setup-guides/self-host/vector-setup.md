# Vector — Self-Host Quick Setup

Minimal steps to install and run Vector for LogWise on a single instance.

## Prerequisites

```bash
# Install Vector for your OS:
# https://vector.dev/docs/setup/installation/operating-systems/

# Install Protocol Buffers compiler (protoc):
# https://protobuf.dev/installation/
```

## 1) Get configuration files

```bash
git clone https://github.com/ds-horizon/logwise.git
cd logwise
cp ./vector/logwise-vector.proto .
cp ./vector/vector.toml .
```

## 2) Configure Kafka bootstrap servers

Edit `vector.toml`:

```toml
# Before
bootstrap_servers = "kafka:29092"

# After
bootstrap_servers = "<YOUR_KAFKA_ADDRESS>:<KAFKA_PORT>"
```

## 3) Build protobuf descriptor

```bash
protoc --include_imports --descriptor_set_out=logwise-vector.desc logwise-vector.proto
```

## 4) Install Vector configuration

```bash
sudo mkdir -p /etc/vector
sudo cp vector.toml /etc/vector/vector.toml
sudo cp logwise-vector.desc /etc/vector/logwise-vector.desc
```

## 5) Configure Vector environment

```bash
FILE="/etc/default/vector"
/bin/cat <<EOM >$FILE
VECTOR_WATCH_CONFIG=true
VECTOR_CONFIG=/etc/vector/vector.toml
EOM
```

## Run and verify

```bash
# Start as a service (Linux)
sudo systemctl start vector
sudo systemctl enable vector

# Or run in foreground
vector --config /etc/vector/vector.toml

# Verify
sudo systemctl status vector
sudo journalctl -u vector -f
```

Vector will accept OTLP HTTP logs on port 4318 and forward to Kafka using your configured bootstrap servers.

# Vector — Self-Host Setup Guide

Follow these steps to get Vector running on your instance for LogWise. This guide extracts only the actionable setup steps.

## Prerequisites

Install the following on your instance:

```bash
# Vector (choose your operating system)
# See: https://vector.dev/docs/setup/installation/operating-systems/

# Protocol Buffers compiler (protoc)
# See: https://protobuf.dev/installation/
```

## Steps

### 1) Clone repository
```bash
git clone https://github.com/ds-horizon/logwise.git
cd logwise
```

### 2) Prepare working directory and copy Vector config files
```bash
# From repo root or adjust paths accordingly
cp ./vector/logwise-vector.proto .
cp ./vector/vector.toml .
```

### 3) Set Kafka broker address in `vector.toml`
Edit `vector.toml` and update the Kafka bootstrap servers. Use comma-separated values for multiple brokers.
```toml
# Before
bootstrap_servers = "kafka:29092"

# After
bootstrap_servers = "<YOUR_KAFKA_ADDRESS>:<KAFKA_PORT>"
```

### 4) Compile the protobuf descriptor
```bash
protoc --include_imports --descriptor_set_out=logwise-vector.desc logwise-vector.proto
```

### 5) Install Vector configuration
```bash
sudo mkdir -p /etc/vector
sudo cp vector.toml /etc/vector/vector.toml
sudo cp logwise-vector.desc /etc/vector/logwise-vector.desc
```

### 6) Configure Vector environment
```bash
FILE="/etc/default/vector"

/bin/cat <<EOM >$FILE
VECTOR_WATCH_CONFIG=true
VECTOR_CONFIG=/etc/vector/vector.toml
EOM
```

## Run Vector

Start Vector as a service (recommended) or run in the foreground for development/testing.
```bash
# Production: systemd service (Linux)
sudo systemctl start vector
sudo systemctl enable vector

# Development: run manually
vector --config /etc/vector/vector.toml
```

## Verify

Check that Vector is up and processing logs.
```bash
# Service status
sudo systemctl status vector

# Live logs
sudo journalctl -u vector -f
```

If Vector starts successfully and shows no connection errors, your instance is ready to receive OTLP logs (HTTP 4318) and forward them to Kafka.


