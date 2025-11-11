# Vector — Self-Host Setup Guide

Vector will accept OTLP HTTP logs on port 4318 and forward to Kafka using your configured bootstrap servers. Follow these steps to get Vector running on your instance for LogWise. This guide extracts only the actionable setup steps.

## Prerequisites

Install the following on your instance:

- **Vector**: [Installation Guide](https://vector.dev/docs/setup/installation/operating-systems/) (choose your operating system)
- **Protocol Buffers compiler (protoc)**: [Installation Guide](https://protobuf.dev/installation/) (only required at build time, not runtime)

## Network Configuration

Vector needs to be accessible on port **4318** (OTLP HTTP) to receive logs from OpenTelemetry collectors.

### Cloud Instances (AWS/GCP/Azure)
Ensure your security group or firewall rules allow inbound traffic on port 4318:
- **AWS**: Add inbound rule in Security Group for port 4318 (TCP) from your source IPs
- **GCP**: Add firewall rule allowing tcp:4318 from your source network
- **Azure**: Add inbound security rule for port 4318 in Network Security Group

### Self-Hosted/On-Premises
Configure your firewall to allow incoming connections on port 4318:
```bash
# Example: UFW (Ubuntu/Debian)
sudo ufw allow 4318/tcp

# Example: firewalld (RHEL/CentOS)
sudo firewall-cmd --permanent --add-port=4318/tcp
sudo firewall-cmd --reload
```

## Steps

### 1) Clone repository
```bash
git clone https://github.com/ds-horizon/logwise.git
cd logwise/vector/
```

### 2) Set Kafka broker address in [`vector.toml`](.././vector/vector.toml)
Edit [`vector.toml`](https://github.com/ds-horizon/logwise/blob/main/vector/vector.toml) and update the Kafka bootstrap servers. Use comma-separated values for multiple brokers.
```toml
# Before
bootstrap_servers = "kafka:29092"

# After
bootstrap_servers = "<YOUR_KAFKA_ADDRESS>:<KAFKA_PORT>"
```

### 3) Compile the protobuf descriptor
From the `logwise/vector/` directory (where you are after Step 1), run:
```bash
protoc --include_imports --descriptor_set_out=logwise-vector.desc logwise-vector.proto
```

### 4) Install Vector configuration
```bash
sudo mkdir -p /etc/vector
sudo cp vector.toml /etc/vector/vector.toml
sudo cp logwise-vector.desc /etc/vector/logwise-vector.desc
```

### 5) Configure Vector environment

Create or edit the Vector environment file `/etc/default/vector` with the following content to enable configuration watching and specify the config file path:

```text
VECTOR_WATCH_CONFIG=true
VECTOR_CONFIG=/etc/vector/vector.toml
```

You can create this file using:
```bash
FILE="/etc/default/vector"

sudo /bin/cat <<EOM >$FILE
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


