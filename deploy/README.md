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

## 📋 Prerequisites

### Required
- **Docker** (v20.10+) and **Docker Compose** (v2.0+)
- **Make** (for convenience commands)
- **AWS Credentials** with access to:
  - S3 bucket (read/write)
  - Athena workgroup (query execution)

### Optional
- **Maven 3.2+** (if building Spark JAR locally)
- **Java 11+** (if building Spark JAR locally)

### Quick Install Missing Tools

```bash
# Run the bootstrap script to install prerequisites
./start.sh

# Or use Make
make bootstrap
```

## 🚀 Quick Start

### 1. Clone and Navigate

```bash
cd log-wise-deploy
```

### 2. Configure Environment

Create a `.env` file from the example (if available) or create one with these variables:

```bash
# AWS Configuration
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_SESSION_TOKEN=your-session-token  # Optional, for temporary credentials

# S3 Configuration
S3_BUCKET=your-bucket-name
S3_PREFIX=logs/

# Athena Configuration
S3_ATHENA_OUTPUT=s3://your-bucket/athena-output/
ATHENA_WORKGROUP=primary
ATHENA_CATALOG=AwsDataCatalog
ATHENA_DATABASE=logwise

# Kafka Configuration
KAFKA_BROKERS=kafka:9092
KAFKA_TOPIC=logs

# Spark Configuration
SPARK_MASTER_URL=spark://spark-master:7077
SPARK_STREAMING=true
SPARK_DRIVER_MEMORY=512m
SPARK_EXECUTOR_MEMORY=512m
SPARK_DRIVER_CORES=1
SPARK_EXECUTOR_CORES=1

# Grafana Configuration
GRAFANA_PORT=3000
GF_SECURITY_ADMIN_USER=admin
GF_SECURITY_ADMIN_PASSWORD=admin
GF_INSTALL_PLUGINS=grafana-athena-datasource

# Orchestrator Configuration
ORCH_PORT=8080

# Tenant Configuration
TENANT_VALUE=D11-Prod-AWS
```

### 3. Start All Services

```bash
make up
```

This will:
- Build custom Spark 3.1.2 Docker image
- Start all services (Vector, Kafka, Spark, Grafana, Orchestrator, MySQL)
- Wait for health checks to pass

### 4. Create Kafka Topic

```bash
make topics
```

### 5. Verify Services

```bash
# Check service status
make ps

# View logs
make logs

# Check specific service logs
docker compose logs spark-master
docker compose logs spark-worker
docker compose logs kafka
```

## 📊 Accessing Services

| Service | URL | Credentials |
|---------|-----|-------------|
| **Grafana** | http://localhost:3000 | `admin` / `admin` (default) |
| **Spark Master UI** | http://localhost:18080 | - |
| **Spark Worker UI** | http://localhost:8081 | - |
| **Orchestrator** | http://localhost:8080 | - |
| **Orchestrator Health** | http://localhost:8080/actuator/health | - |

## 🔧 Running Spark Jobs

### Option 1: Auto-Submit (Default)

The `spark-client` container automatically submits the Spark job on startup via REST API. Check logs:

```bash
docker compose logs spark-client
```

### Option 2: Manual Submit via REST API

```bash
make spark-rest-submit
```

### Option 3: Direct Spark Submit

```bash
make spark-submit
```

### Building and Running Custom JAR

```bash
# Build the Spark application JAR
make spark-build

# Submit custom JAR
make spark-run-jar MAIN_CLASS=com.dream11.MainApplication APP_JAR=your-app.jar
```

## 🧪 Testing the Pipeline

### 1. Test Vector → Kafka

Create a sample log file:

```bash
echo '{"level":"INFO","message":"test log","app":"demo","ts":"2025-01-01T00:00:00Z"}' >> vector/demo/test.log
```

Verify Kafka received the message:

```bash
docker compose exec kafka kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 \
  --topic logs \
  --from-beginning \
  --timeout-ms 2000
```

### 2. Test Spark → S3

After submitting a Spark job, verify Parquet files in S3:

```bash
aws s3 ls s3://${S3_BUCKET}/${S3_PREFIX}/
```

### 3. Test Athena → Grafana

1. Ensure Athena table exists pointing to S3 path
2. Open Grafana → Explore → Select Athena datasource
3. Run query: `SELECT * FROM your_table LIMIT 10`

### 4. Test Orchestrator

```bash
curl http://localhost:8080/actuator/health
```

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

# Create Kafka topic
make topics

# Submit Spark job
make spark-submit

# Submit Spark job via REST API
make spark-rest-submit

# Build Spark JAR
make spark-build

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
log-wise-deploy/
├── docker-compose.yml          # Main orchestration file
├── Makefile                    # Convenience commands
├── .env                        # Environment variables (create this)
├── spark/
│   ├── Dockerfile              # Custom Spark 3.1.2 image
│   ├── rest-submit.sh          # REST API submission script
│   └── submit.sh               # Direct submission script
├── vector/
│   ├── vector.yaml             # Vector configuration
│   └── logcentral_logs.desc    # Protobuf schema
├── d11-log-management-spark/   # Spark application source
│   └── src/main/java/          # Java source code
├── log-central-orchestrator/   # Spring Boot orchestrator
├── grafana/
│   └── provisioning/           # Grafana datasource config
└── db/
    └── init/                   # Database initialization scripts
```

## 🔐 Security Notes

- **Never commit `.env` file** - Contains sensitive AWS credentials
- Use **IAM roles** in production instead of access keys
- Enable **TLS/SSL** for production deployments
- Restrict **network access** to services in production

## 📚 Additional Resources

- [Architecture Documentation](../docs/architecture-overview.md)
- [Component Guides](../docs/components/)
- [Setup Guides](../docs/setup-guides/)
- [Spark Application README](./d11-log-management-spark/README.md)

## 🤝 Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

## 📝 License

This project is licensed under the GNU Lesser General Public License v3.0. See [LICENSE](../LICENSE) for details.

## 💬 Support

For issues or questions:
- Open an issue on GitHub
- Check existing issues for solutions
- Review logs: `make logs` or `docker compose logs <service>`

---

**Happy Logging! 🚀**
