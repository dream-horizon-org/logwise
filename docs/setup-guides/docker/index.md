# LogWise - Local Development Setup

A complete end-to-end logging system that streams logs from Vector → Kafka → Spark → S3/Athena, with a Spring Boot Orchestrator, Grafana dashboards, and automated cron jobs.

## 🏗️ Architecture

```
┌─────────┐      ┌─────────┐      ┌─────────┐      ┌─────────┐
│ Vector  │─────▶│  Kafka  │─────▶│  Spark  │─────▶│   S3    │
│ (Logs)  │      │(Stream) │      │(Process)│      │(Storage)│
└─────────┘      └─────────┘      └─────────┘      └─────────┘
                                                           │
                                                           ▼
                                                  ┌─────────────┐
                                                  │   Athena    │
                                                  │  (Query)    │
                                                  └─────────────┘
                                                           │
                                                           ▼
                                                  ┌─────────────┐
                                                  │   Grafana   │
                                                  │ (Dashboard) │
                                                  └─────────────┘
```

**Components:**
- **Vector**: Log collection and forwarding
- **Kafka**: Message streaming (KRaft mode)
- **Spark 3.1.2**: Stream processing and Parquet writing
- **S3**: Object storage for processed logs
- **Athena**: Query engine for S3 data
- **Grafana**: Visualization and dashboards
- **Orchestrator**: Spring Boot service for job management
- **MySQL**: Database for orchestrator configuration

> **Note on log collectors**  
> In the Docker-based Logwise stack, logs are shipped into Vector using the **OpenTelemetry Collector (OTEL)** over OTLP by default.  
> If you prefer to use other agents such as Fluent Bit, Fluentd, Logstash, or syslog-ng/rsyslog, see the **Send Logs → Log collectors** section for agent-specific setup guides and the required Vector configuration changes.

## 📋 Prerequisites

### Required
- **Docker** (v20.10+) and **Docker Compose** (v2.0+)
- **Make** (for convenience commands)
- **AWS Credentials** with access to:
  - S3 bucket (read/write)
  - Athena workgroup (query execution)

> **Note:** The `setup.sh` script will automatically install Docker, Make, and other prerequisites if they're missing (on macOS and Debian/Ubuntu Linux). For other systems, install these manually before running setup.

### Optional
- **Maven 3.2+** (if building Spark JAR locally)
- **Java 11+** (if building Spark JAR locally)

## ⚠️ Mandatory: S3 & Athena Setup (Must Complete First)

**Before proceeding with the Docker setup, you MUST complete the S3 & Athena configuration.** This is a required prerequisite as the LogWise stack depends on AWS S3 for log storage and Athena for querying.

### Steps to Complete:

1. **Follow the [S3 & Athena Setup Guide](../self-host/s3-athena-setup.md)** to:
   - Create an S3 bucket with `logs` and `athena-output` folders
   - Create an AWS Glue database
   - Create an Athena workgroup
   - Create the `application-logs` table

2. **Note down the following information** (you'll need it for the `.env` file):
   - S3 bucket name
   - S3 URI for logs (e.g., `s3://your-bucket-name/logs/`)
   - S3 URI for Athena output (e.g., `s3://your-bucket-name/athena-output/`)
   - Athena workgroup name
   - Athena database name (typically `logs`)

3. **Return to this page** after completing the S3 & Athena setup to continue with the Docker deployment.

::: warning Critical
**Do not proceed with the Docker setup until you have completed the S3 & Athena configuration.** The setup will fail without proper AWS resources configured.
:::

## 🚀 Quick Start

### One-Command Setup

The easiest way to get started is with our one-click setup script:

```bash
cd deploy
./setup.sh
```

This single command will:
- ✅ Install prerequisites (Docker, Make, AWS CLI, etc.) if needed
- ✅ Create `.env` file from template (`.env.example`)
- ✅ Prompt you to fill in AWS credentials
- ✅ Start all services (Vector, Kafka, Spark, Grafana, Orchestrator, MySQL)
- ✅ Wait for services to become healthy
- ✅ Create Kafka topics automatically

**That's it!** Your LogWise stack will be up and running.


## 📊 Accessing Services

| Service | URL | Credentials |
|---------|-----|-------------|
| **Grafana** | `http://localhost:3000` | `admin` / `admin` (default) |
| **Spark Master UI** | `http://localhost:18080` | - |
| **Spark Worker UI** | `http://localhost:8081` | - |
| **Orchestrator** | `http://localhost:8080` | - |
| **Orchestrator Health** | `http://localhost:8080/healthcheck` | - |

## ⚙️ Configuration Details

The `.env` file contains all configuration for the LogWise stack. When you run `setup.sh`, it automatically creates this file from `.env.example`. Here are the key configuration sections:

### AWS Configuration (Required)

```bash
AWS_REGION=us-east-1                    # AWS region for S3 and Athena
AWS_ACCESS_KEY_ID=your-access-key       # AWS access key ID
AWS_SECRET_ACCESS_KEY=your-secret-key   # AWS secret access key
AWS_SESSION_TOKEN=                      # Optional: for temporary credentials
```

### S3 Configuration (Required)

```bash
S3_BUCKET_NAME=your-bucket-name              # S3 bucket for storing processed logs
S3_PREFIX=logs/                         # Prefix/path within the bucket
```

### Athena Configuration (Required)

```bash
S3_ATHENA_OUTPUT=s3://bucket/athena-output/  # S3 path for Athena query results
ATHENA_WORKGROUP=primary                     # Athena workgroup name
ATHENA_CATALOG=AwsDataCatalog               # Athena data catalog
ATHENA_DATABASE=logwise                     # Athena database name
```

### Kafka Configuration

```bash
KAFKA_BROKERS=kafka:9092                 # Kafka broker address (default for Docker)
KAFKA_TOPIC=logs                         # Kafka topic name for logs
KAFKA_CLUSTER_ID=9ZkYwXlQ2Tq8rBn5JcH0xA  # Kafka cluster ID (KRaft mode)
```

### Spark Configuration

```bash
SPARK_MASTER_URL=spark://spark-master:7077  # Spark master URL
SPARK_STREAMING=true                        # Enable Spark streaming
SPARK_MASTER_UI_PORT=18080                  # Spark Master UI port
SPARK_VERSION_MATCH=3.1.2                   # Spark version
HADOOP_AWS_VERSION=3.2.0                    # Hadoop AWS library version
AWS_SDK_VERSION=1.11.375                   # AWS SDK version
MAIN_CLASS=com.logwise.spark.MainApplication      # Spark application main class
```

### Database Configuration

```bash
MYSQL_DATABASE=myapp                       # MySQL database name
MYSQL_USER=myapp                           # MySQL user
MYSQL_PASSWORD=myapp_pass                  # MySQL password
MYSQL_ROOT_PASSWORD=root_pass              # MySQL root password
```

### Other Configuration

```bash
ORCH_PORT=8080                             # Orchestrator service port
TENANT_VALUE=ABC               # Tenant identifier
```

For a complete list of all environment variables, see `.env.example` in the deploy directory.


## 🛠️ Common Commands

```bash
# Start all services
make up

# Stop all services
make down

# View logs
make logs

# Check service status
make ps

# Stop and remove volumes
make teardown

# Reset Kafka (fix cluster ID issues)
make reset-kafka
```

## ⚠️ Troubleshooting

### Spark Worker Not Accepting Resources

**Symptom:** `WARN Master: App requires more resource than any of Workers could have`

**Solution:**
1. Check worker memory: `docker compose logs spark-worker | grep "Starting Spark worker"`
2. Ensure worker has enough memory. The worker needs:
   - Memory for driver + executor + overhead
   - Default: 512m driver + 512m executor = ~1GB minimum
3. Adjust in `.env`:
   ```bash
   SPARK_DRIVER_MEMORY=400m
   SPARK_EXECUTOR_MEMORY=400m
   ```
4. Or increase worker memory limit in `docker-compose.yml`:
   ```yaml
   spark-worker:
     mem_limit: 3g
   ```

### ClassNotFoundException for S3 or Kafka

**Symptom:** `java.lang.ClassNotFoundException: org.apache.hadoop.fs.s3a.S3AFileSystem`

**Solution:**
- The custom Spark Dockerfile includes required JARs:
  - `hadoop-aws-3.2.0.jar`
  - `aws-java-sdk-bundle-1.11.375.jar`
  - `spark-sql-kafka-0-10_2.12-3.1.2.jar`
  - `kafka-clients-2.6.0.jar`
- Rebuild the Spark image: `docker compose build spark-worker spark-master spark-client`

### AWS Access Denied (403 Forbidden)

**Symptom:** `AccessDeniedException: 403 Forbidden`

**Solution:**
1. Verify AWS credentials in `.env`:
   ```bash
   AWS_ACCESS_KEY_ID=your-key
   AWS_SECRET_ACCESS_KEY=your-secret
   AWS_SESSION_TOKEN=your-token  # If using temporary credentials
   AWS_REGION=us-east-1
   ```
2. Ensure IAM permissions include:
   - `s3:GetObject`, `s3:PutObject`, `s3:ListBucket` on target bucket
   - `athena:StartQueryExecution`, `athena:GetQueryResults` (if using Athena)
3. Restart Spark client: `docker compose restart spark-client`

### Port Conflicts

**Symptom:** `Error: bind: address already in use`

**Solution:**
- Change ports in `.env`:
  ```bash
  GRAFANA_PORT=3001
  ORCH_PORT=8081
  ```

### Kafka Cluster ID Mismatch

**Symptom:** `Cluster ID mismatch` errors

**Solution:**
```bash
make reset-kafka
make up
```

### Disk Space Issues

**Symptom:** `no space left on device`

**Solution:**
```bash
# Clean up Docker
docker system prune -a --volumes

# Remove unused images
docker image prune -a
```

### Spark Worker Not Registering

**Symptom:** Worker fails to connect to master

**Solution:**
1. Check network connectivity:
   ```bash
   docker compose exec spark-worker curl http://spark-master:8080
   ```
2. Verify master is running:
   ```bash
   docker compose logs spark-master | grep "Successfully started service"
   ```
3. Check worker logs:
   ```bash
   docker compose logs spark-worker | grep -i "error\|exception"
   ```

## 📁 Project Structure

```
logwise/
├── deploy/
│   ├── docker-compose.yml       # Main orchestration file
│   ├── Makefile                 # Convenience commands
│   ├── setup.sh                 # One-click setup script
│   ├── grafana/provisioning/    # Grafana dashboards & datasources
│   └── healthcheck-dummy/
│       └── Dockerfile           # Healthcheck test service
├── vector/
│   ├── vector.yaml              # Vector configuration
│   └── logwise-vector.desc      # Protobuf descriptor
├── spark/
│   └── docker/Dockerfile        # Spark container image
└── orchestrator/
    ├── docker/Dockerfile        # Orchestrator container image
    └── db/init/                 # Database initialization scripts
```

## 🔐 Security Notes

- **Never commit `.env` file** - Contains sensitive AWS credentials
- Use **IAM roles** in production instead of access keys
- Enable **TLS/SSL** for production deployments
- Restrict **network access** to services in production


**Happy Logging! 🚀**