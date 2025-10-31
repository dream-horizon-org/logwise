# 🚀 Vector.dev Setup and Configuration Guide

This comprehensive guide covers setting up Vector.dev on a machine, configuring it with `vector.toml`, and handling distributed deployments where Vector and Kafka run on separate machines.

## 📋 Table of Contents

1. [Introduction to Vector.dev](#introduction-to-vectordev)
2. [Installation](#installation)
3. [Basic Configuration](#basic-configuration)
4. [vector.toml Structure](#vectortoml-structure)
5. [Distributed Setup (Vector + Kafka on Separate Machines)](#distributed-setup)
6. [Production Considerations](#production-considerations)
7. [Troubleshooting](#troubleshooting)

## 🌟 Introduction to Vector.dev

Vector is a high-performance, end-to-end (E2E) observability data pipeline that puts you in control of your observability data. It's built in Rust for maximum performance and reliability.

### ⚠️ **IMPORTANT: Minimum Version Requirement**

Vector version with protobuf codec support is required. **Versions ≥ 0.45.0** are known to include it; verify in your build. If unavailable, use codec = "json".

### Key Benefits:
- ✅ **High Performance**: Rust-based processing engine
- ✅ **Vendor Neutral**: Works with any observability backend
- ✅ **Rich Transformations**: Built-in VRL (Vector Remap Language)
- ✅ **Production Ready**: Robust error handling and monitoring
- ✅ **Protocol Support**: OpenTelemetry, syslog, JSON, and more

## 🛠️ Installation

### Option 1: Docker (Recommended for Development)

#### Quick Start with Pre-built Image
```bash
# Pull Vector image version >= 0.45.0 (REQUIRED for protobuf support)
docker pull timberio/vector:0.45.0-distroless-libc

# Verify version
docker run --rm timberio/vector:0.45.0-distroless-libc --version

# ⚠️ NOTE: Pre-built images don't include protoc
# For protobuf codec support, you need a custom image (see Basic Configuration section)

# Run with mounted configuration (JSON codec only)
docker run -d \
  --name vector \
  -p 4317:4317 \
  -p 4318:4318 \
  -p 8686:8686 \
  -v $(pwd)/vector.toml:/etc/vector/vector.toml \
  -v $(pwd)/data:/var/lib/vector \
  timberio/vector:0.45.0-distroless-libc
```

#### Custom Image with Protobuf Support (Recommended)
For protobuf codec support, build a custom image as shown in the Basic Configuration section.

### Option 2: Binary Installation (Linux/macOS)

```bash
# ⚠️ IMPORTANT: Always verify version >= 0.45.0 after installation

# Linux (using curl) - installs latest version
curl --proto '=https' --tlsv1.2 -sSf https://sh.vector.dev | bash

# macOS (using Homebrew) - installs latest version
brew install vector

# Ubuntu/Debian
curl -1sLf 'https://repositories.timber.io/public/vector/cfg/setup/bash.deb.sh' | sudo -E bash
sudo apt install vector

# CentOS/RHEL/Fedora
curl -1sLf 'https://repositories.timber.io/public/vector/cfg/setup/bash.rpm.sh' | sudo -E bash
sudo yum install vector

# Verify installation version (MUST be >= 0.45.0)
vector --version

# If version is < 0.45.0, upgrade to latest:
# For Homebrew: brew upgrade vector
# For apt: sudo apt update && sudo apt upgrade vector
# For yum: sudo yum update vector
```

### Option 3: Package Manager Installation

```bash
# ⚠️ IMPORTANT: Ensure package repositories have Vector >= 0.45.0

# Arch Linux
pacman -S vector

# FreeBSD
pkg install vector

# Windows (using Chocolatey)
choco install vector

# Always verify version after installation
vector --version

# If your package manager has an older version, use Docker or direct binary installation instead
```

### ⚠️ **Critical Version Check**

After installation, **ALWAYS** verify the Vector version:

```bash
# Check Vector version
vector --version

# Expected output should be >= 0.45.0
# Example: vector 0.45.0 (x86_64-unknown-linux-gnu)
```

**If your version is < 0.45.0:**
- Protobuf codec may NOT work
- Use Docker with specific version tag
- Or download binary directly from [Vector releases](https://github.com/vectordotdev/vector/releases)

## 🔧 Protocol Buffers Setup (Required for Kafka Integration)

Since this configuration uses protobuf codec for efficient Kafka message serialization, you need to set up the necessary files.

### ⚡ **Key Point: Build-time vs Runtime Requirements**

- **protoc (Protocol Buffers compiler)**: Only needed at **BUILD TIME** to generate `.desc` file
- **Runtime**: Only requires the pre-generated `.desc` file - protoc is NOT needed
- **Docker Benefit**: Runtime containers can be distroless if `.desc` is baked in during build

### Install Protocol Buffers Compiler (protoc)

#### Linux (Ubuntu/Debian)
```bash
# Install protobuf compiler
sudo apt update
sudo apt install -y protobuf-compiler

# Verify installation
protoc --version
```

#### Linux (CentOS/RHEL/Fedora)
```bash
# CentOS/RHEL
sudo yum install -y protobuf-compiler
# or for newer versions
sudo dnf install -y protobuf-compiler

# Verify installation
protoc --version
```

#### macOS
```bash
# Using Homebrew
brew install protobuf

# Verify installation
protoc --version
```

#### Windows
```bash
# Using Chocolatey
choco install protoc

# Using winget
winget install protobuf

# Verify installation
protoc --version
```

### Required Files Setup

You need two essential files for protobuf integration:

1. **`logcentral_logs.proto`** - Protocol Buffers schema definition
2. **`vector.toml`** - Vector configuration file

#### Sample logcentral_logs.proto
```protobuf
syntax = "proto3";

package logcentral.logs;

message VectorLogs {
  string service_name = 1;
  string environment_name = 2;
  string message = 3;
  string log_level = 4;
  string timestamp = 5;
}
```

## ⚙️ Basic Configuration

### 1. File Setup and Directory Structure

#### For VM/Bare Metal Installation

```bash
# Create necessary directories
sudo mkdir -p /etc/vector
sudo mkdir -p /var/lib/vector

# Set proper permissions (if running as vector user)
sudo chown vector:vector /var/lib/vector

# Copy your configuration files
sudo cp vector.toml /etc/vector/vector.toml
sudo cp logcentral_logs.proto /etc/vector/logcentral_logs.proto

# Generate protobuf descriptor file (REQUIRED for protobuf codec)
cd /etc/vector
sudo protoc --proto_path=. --descriptor_set_out=logcentral_logs.desc --include_imports logcentral_logs.proto

# Verify files are in place
ls -la /etc/vector/
# Should show: vector.toml, logcentral_logs.proto, logcentral_logs.desc
```

#### For Docker Deployment

Create a Dockerfile that includes protoc for **build time** and generates the `.desc` file. The runtime container only needs the `.desc` file:

```dockerfile
FROM debian:12-slim

WORKDIR /app

# Install dependencies including protobuf compiler
RUN apt-get update && apt-get install -y \
    curl \
    ca-certificates \
    protobuf-compiler \
    && rm -rf /var/lib/apt/lists/*

# Install Vector (version >= 0.45.0)
RUN ARCH=$(uname -m) && \
    if [ "$ARCH" = "x86_64" ]; then ARCH="x86_64"; elif [ "$ARCH" = "aarch64" ] || [ "$ARCH" = "arm64" ]; then ARCH="aarch64"; fi && \
    curl -L "https://github.com/vectordotdev/vector/releases/download/v0.45.0/vector-0.45.0-${ARCH}-unknown-linux-musl.tar.gz" \
    -o vector.tar.gz && \
    tar -xzf vector.tar.gz && \
    mv vector-*/bin/vector /usr/local/bin/vector && \
    chmod +x /usr/local/bin/vector && \
    rm -rf vector.tar.gz vector-*

# Copy Vector configuration and protobuf schema
COPY vector.toml /etc/vector/vector.toml
COPY logcentral_logs.proto /etc/vector/logcentral_logs.proto

# Generate protobuf descriptor file (CRITICAL STEP - BUILD TIME ONLY)
RUN cd /etc/vector && \
    protoc --proto_path=. --descriptor_set_out=logcentral_logs.desc --include_imports logcentral_logs.proto

# Expose ports
EXPOSE 4317 4318 8686

# Run Vector
CMD ["vector", "--config", "/etc/vector/vector.toml"]
```

**🏗️ Build Process Explanation:**
- **protoc** installs and runs during Docker build (line 7, 14-15)
- **`.desc` file** generates at build time (line 14-15)  
- **Runtime**: Only needs the `.desc` file - protobuf compiler is not used
- **Optimization**: For production, you could use a multi-stage build with a distroless runtime image

**Build and run:**
```bash
# Build Docker image
docker build -t vector-protobuf .

# Run container with volume mounts
docker run -d \
  --name vector \
  -p 4317:4317 \
  -p 4318:4318 \
  -p 8686:8686 \
  -v $(pwd)/data:/var/lib/vector \
  vector-protobuf
```

### ⚠️ **Critical File Requirements**

For protobuf codec to work, ensure these files exist:

| **File** | **Location** | **Build Time** | **Runtime** | **Purpose** |
|----------|--------------|----------------|-------------|-------------|
| `vector.toml` | `/etc/vector/vector.toml` | ✅ | ✅ | Vector configuration |
| `logcentral_logs.proto` | `/etc/vector/logcentral_logs.proto` | ✅ | ❌ | Protobuf schema source |
| `logcentral_logs.desc` | `/etc/vector/logcentral_logs.desc` | ✅ | ✅ | Compiled protobuf descriptor |

**Key Points:**
- **`.desc` file** is auto-generated from `.proto` using protoc and is REQUIRED for Vector protobuf codec
- **Build time**: Needs `.proto` + protoc to generate `.desc`
- **Runtime**: Only needs `.desc` file - `.proto` and protoc can be removed for distroless images

### 2. Start Vector

```bash
# With systemd (Linux)
sudo systemctl start vector
sudo systemctl enable vector

# Manual start
vector --config /etc/vector/vector.toml

# Docker (ensure version >= 0.45.0 for protobuf support)
docker run -d \
  --name vector \
  -p 4317:4317 \
  -p 4318:4318 \
  -p 8686:8686 \
  -v /path/to/vector.toml:/etc/vector/vector.toml \
  timberio/vector:0.45.0-distroless-libc
```

## 📝 vector.toml Structure

The `vector.toml` file is the main configuration file for Vector. Here's a comprehensive breakdown:

### Basic Structure

```toml
# Global configuration
data_dir = "/var/lib/vector"

# API configuration for monitoring and control
[api]
enabled = true
address = "0.0.0.0:8686"
playground = false

# Sources - where data comes from
[sources.SOURCE_NAME]
type = "source_type"
# source-specific configuration

# Transforms - how to process data
[transforms.TRANSFORM_NAME]
type = "transform_type"
inputs = ["source_or_transform_name"]
# transform-specific configuration

# Sinks - where data goes
[sinks.SINK_NAME]
type = "sink_type"
inputs = ["source_or_transform_name"]
# sink-specific configuration
```

### Essential Configuration Requirements

For protobuf integration, you need the **`.desc` file** (compiled protobuf descriptor):

```bash
# Generate required .desc file (build-time only)
protoc --proto_path=. --descriptor_set_out=logcentral_logs.desc --include_imports logcentral_logs.proto
```

Key configuration points:
- **Kafka brokers**: `bootstrap_servers = "kafka-broker1.example.com:9092,kafka-broker2.example.com:9092"`
- **Protobuf codec**: Requires Vector ≥ 0.45.0 and the `.desc` file
- **Buffer configuration**: Adjust based on throughput requirements

## 🏢 Distributed Setup (Vector + Kafka on Separate Machines)

When Vector and Kafka run on separate machines, you need to modify the configuration for network connectivity, security, and reliability.

### Configuration Changes Required

#### Prerequisites for Separate Machines

**Both Vector and Kafka machines need:**
1. **Vector >= 0.45.0** (for protobuf support)
2. **Protocol Buffers compiler** (`protoc`) installed on Vector machine
3. **Required files** on Vector machine:
   - `vector.toml` configuration file
   - `logcentral_logs.proto` schema file  
   - `logcentral_logs.desc` compiled descriptor file

```bash
# On Vector machine - ensure protobuf setup
protoc --version
ls -la /etc/vector/
# Should show: vector.toml, logcentral_logs.proto, logcentral_logs.desc
```

#### 1. Kafka Connection Configuration

**Key configuration changes for separate machines:**

```toml
# Update Kafka broker addresses
bootstrap_servers = "kafka-broker1.example.com:9092,kafka-broker2.example.com:9092,kafka-broker3.example.com:9092"

# Ensure .desc file is available
encoding.protobuf.desc_file = "/etc/vector/logcentral_logs.desc"
```

#### 2. Environment-based Configuration

For production deployments, use the same base configuration from above but with these key changes:

**Production environment variables:**

```bash
# Required environment variables
KAFKA_BROKERS=kafka-broker1.prod.com:9092,kafka-broker2.prod.com:9092,kafka-broker3.prod.com:9092
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=SCRAM-SHA-256
```

#### 3. Environment Variables Setup

```bash
# Create environment file
sudo tee /etc/vector/vector.env << EOF
KAFKA_BROKERS=kafka-broker1.example.com:9092,kafka-broker2.example.com:9092,kafka-broker3.example.com:9092
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=SCRAM-SHA-256
KAFKA_USERNAME=vector-user
KAFKA_PASSWORD=your-secure-password
EOF

# Secure the environment file
sudo chmod 600 /etc/vector/vector.env
sudo chown vector:vector /etc/vector/vector.env
```

#### 4. Systemd Service Configuration

```bash
# Create systemd service file
sudo tee /etc/systemd/system/vector.service << EOF
[Unit]
Description=Vector
Documentation=https://vector.dev
After=network-online.target
Requires=network-online.target

[Service]
User=vector
Group=vector
ExecStart=/usr/bin/vector --config /etc/vector/vector.toml
ExecReload=/bin/kill -HUP \$MAINPID
Restart=always
RestartSec=2
EnvironmentFile=/etc/vector/vector.env
LimitNOFILE=8192
KillMode=mixed
KillSignal=SIGTERM
TimeoutStopSec=60

[Install]
WantedBy=multi-user.target
EOF

# Reload systemd and start Vector
sudo systemctl daemon-reload
sudo systemctl enable vector
sudo systemctl start vector
```

### Network Configuration Checklist

#### ⚠️ **CRITICAL: Kafka `advertised.listeners` Configuration**

**Most common cross-machine connectivity issue!** Ensure Kafka brokers advertise the correct hostname/IP:

```properties
# ❌ WRONG: Will only work on localhost
advertised.listeners=PLAINTEXT://localhost:9092

# ✅ CORRECT: Works across networks
advertised.listeners=PLAINTEXT://kafka-broker1.example.com:9092

# Docker example with both internal and external access
advertised.listeners=PLAINTEXT://kafka:29092,PLAINTEXT_HOST://kafka-broker1.example.com:9092
```

#### Firewall Rules
```bash
# On Vector machine - allow incoming connections
sudo ufw allow 4317/tcp  # OTLP gRPC
sudo ufw allow 4318/tcp  # OTLP HTTP
sudo ufw allow 8686/tcp  # Vector API

# On Kafka machines - allow incoming connections from Vector
sudo ufw allow from VECTOR_IP to any port 9092  # Kafka broker
```

#### DNS Resolution
Ensure Vector machine can resolve Kafka hostnames:
```bash
# Test DNS resolution
nslookup kafka-broker1.example.com
nslookup kafka-broker2.example.com

# Or add to /etc/hosts if needed
echo "10.0.1.100 kafka-broker1.example.com" >> /etc/hosts
echo "10.0.1.101 kafka-broker2.example.com" >> /etc/hosts
```

#### Network Connectivity Test
```bash
# Test connectivity to Kafka brokers
telnet kafka-broker1.example.com 9092
telnet kafka-broker2.example.com 9092

# Test with kafka-topics if available
kafka-topics --bootstrap-server kafka-broker1.example.com:9092 --list
```

## 🔒 Production Considerations

### Security

**TLS/SSL and SASL Authentication:**
- Use environment variables for credentials
- Configure security protocol (SSL, SASL_SSL)
- Set appropriate SASL mechanism (SCRAM-SHA-256, PLAIN, etc.)

### Performance Tuning

**Key settings:**
- **Buffer size**: Adjust `max_size` based on throughput (256MB to 2GB)
- **Compression**: Use "zstd" for optimal performance
- **Batch settings**: Configure `linger.ms` and `batch.size` in librdkafka_options

### Monitoring and Health Checks

```bash
# Check Vector API health
curl http://localhost:8686/health

# Monitor Vector logs
journalctl -u vector -f

# Check for Kafka connection issues
journalctl -u vector | grep -i kafka
```

## 🔧 Troubleshooting

### Common Issues and Solutions

#### 1. Version Compatibility Issues

**Problem**: Protobuf codec not working
```
ERROR sink{component_kind="sink" component_id="app_logs_kafka" component_type="kafka"}: vector::sinks::kafka: Unknown encoding codec: protobuf
```

**Solution**:
```bash
# Check Vector version
vector --version

# If version < 0.45.0, upgrade:
# Docker: Use timberio/vector:0.45.0-distroless-libc or later
# Homebrew: brew upgrade vector
# Apt: sudo apt update && sudo apt upgrade vector

# Alternative: Use JSON codec for older versions
# In vector.toml, change:
# codec = "protobuf"
# to:
# codec = "json"
```

#### 2. Protocol Buffers Setup Issues

**Problem**: Missing protobuf descriptor file
```
ERROR sink{component_kind="sink" component_id="app_logs_kafka" component_type="kafka"}: Failed to load protobuf descriptor file
```

**Solution**:
```bash
# Check if protoc is installed
protoc --version

# Install protoc if missing (Ubuntu/Debian)
sudo apt install -y protobuf-compiler

# Generate descriptor file
cd /etc/vector
sudo protoc --proto_path=. --descriptor_set_out=logcentral_logs.desc --include_imports logcentral_logs.proto

# Verify files exist
ls -la /etc/vector/
# Should show: vector.toml, logcentral_logs.proto, logcentral_logs.desc
```

**Problem**: Protobuf schema mismatch
```
ERROR sink{component_kind="sink" component_id="app_logs_kafka" component_type="kafka"}: Protobuf serialization failed
```

**Solution**:
```bash
# Regenerate descriptor file after proto changes
cd /etc/vector
sudo protoc --proto_path=. --descriptor_set_out=logcentral_logs.desc --include_imports logcentral_logs.proto

# Restart Vector
sudo systemctl restart vector
```

**Problem**: Missing .proto file in Docker
```
ERROR: COPY failed: stat logcentral_logs.proto: file does not exist
```

**Solution**:
```bash
# Ensure files are in build context
ls -la
# Should show: Dockerfile, vector.toml, logcentral_logs.proto

# Create the proto file if missing
cat > logcentral_logs.proto << EOF
syntax = "proto3";

package logcentral.logs;

message VectorLogs {
  string service_name = 1;
  string environment_name = 2;
  string message = 3;
  string log_level = 4;
  string timestamp = 5;
}
EOF

# Rebuild Docker image
docker build -t vector-protobuf .
```

**Problem**: Runtime missing protoc (trying to regenerate .desc)
```
ERROR: protoc: command not found
```

**Solution**:
This is actually expected behavior! protoc should NOT be in runtime containers:
```bash
# ✅ CORRECT: Only .desc file needed at runtime
ls /etc/vector/logcentral_logs.desc

# ❌ WRONG: Don't try to run protoc at runtime
# protoc should only run during build time
```

**Remember**: protoc is build-time only; runtime containers only need the pre-generated `.desc` file.

#### 3. Connection Issues to Kafka

**Problem**: Vector cannot connect to Kafka brokers
```
ERROR sink{component_kind="sink" component_id="app_logs_kafka" component_type="kafka"}: vector::sinks::kafka: Failed to produce message: BrokerTransportFailure
```

**⚠️ COMMON GOTCHA: Kafka `advertised.listeners` Configuration**

**The #1 reason "works on localhost, fails cross-machine"** is incorrect Kafka broker configuration:

```bash
# ❌ WRONG: Kafka advertises localhost - only works locally
advertised.listeners=PLAINTEXT://localhost:9092

# ✅ CORRECT: Kafka advertises actual hostname/IP - works across networks
advertised.listeners=PLAINTEXT://kafka1.example.com:9092
# or
advertised.listeners=PLAINTEXT://10.0.1.100:9092
```

**Check Kafka broker configuration:**
```bash
# Connect to Kafka broker and check what it advertises
kcat -b kafka1.example.com:9092 -L

# Look for "broker" entries - they show what Kafka advertises:
# metadata for 1 brokers:
#   broker 1 at localhost:9092 (id: 1 rack: )  ← ❌ BAD: advertises localhost
#   broker 1 at kafka1.example.com:9092 (id: 1 rack: )  ← ✅ GOOD: advertises hostname
```

**Fix Kafka broker configuration:**
```properties
# In Kafka server.properties or docker-compose.yml
# Single listener (simple setup)
listeners=PLAINTEXT://0.0.0.0:9092
advertised.listeners=PLAINTEXT://kafka1.example.com:9092

# Multiple listeners (Docker + external access)
listeners=PLAINTEXT://0.0.0.0:29092,PLAINTEXT_HOST://0.0.0.0:9092
advertised.listeners=PLAINTEXT://kafka:29092,PLAINTEXT_HOST://kafka1.example.com:9092
listener.security.protocol.map=PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
```

**Other Network Troubleshooting:**
```bash
# Check network connectivity
ping kafka1.example.com
telnet kafka1.example.com 9092

# Verify DNS resolution
nslookup kafka1.example.com

# Check firewall rules
sudo ufw status
sudo iptables -L

# Test with kcat
kcat -b kafka1.example.com:9092 -L
```

#### 4. Authentication Failures

**Problem**: SASL authentication failures
```
ERROR sink{component_kind="sink" component_id="app_logs_kafka" component_type="kafka"}: vector::sinks::kafka: Failed to produce message: SaslAuthenticationFailed
```

**Solutions**:
```bash
# Verify credentials
echo $KAFKA_USERNAME
echo $KAFKA_PASSWORD  # Be careful with this

# Test authentication with kcat
kcat -b kafka-broker1.example.com:9092 -X security.protocol=SASL_SSL \
  -X sasl.mechanism=SCRAM-SHA-256 \
  -X sasl.username=$KAFKA_USERNAME \
  -X sasl.password=$KAFKA_PASSWORD -L
```

#### 5. High Memory Usage

**Solutions**: Reduce buffer sizes, enable compression (zstd)

#### 6. Message Ordering Issues

**Solutions**: Set `max.in.flight.requests.per.connection = "1"` and `enable.idempotence = "true"`

### Debugging Configuration

```bash
# Enable debug logging
export VECTOR_LOG=debug

# Validate configuration
vector validate /etc/vector/vector.toml

# Test individual components
vector test /etc/vector/vector.toml
```

## 📚 Additional Resources

- [Vector.dev Official Documentation](https://vector.dev/docs/)
- [VRL (Vector Remap Language) Reference](https://vector.dev/docs/reference/vrl/)
- [Kafka Sink Configuration](https://vector.dev/docs/reference/configuration/sinks/kafka/)
- [OpenTelemetry Source Configuration](https://vector.dev/docs/reference/configuration/sources/opentelemetry/)

## 🤝 Support

For issues and questions:
1. Check Vector logs: `journalctl -u vector -f`
2. Validate configuration: `vector validate /etc/vector/vector.toml`
3. Review Vector documentation: [vector.dev](https://vector.dev)
4. Community support: [Vector Discord](https://discord.gg/dX3bdkF)

---

*This documentation covers the complete setup and configuration of Vector.dev for log processing and Kafka integration, with special attention to distributed deployments.*
