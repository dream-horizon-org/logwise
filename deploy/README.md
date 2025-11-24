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

> **Note:** The `setup.sh` script will automatically install Docker, Make, and other prerequisites if they're missing (on macOS and Debian/Ubuntu Linux). For other systems, install these manually before running setup.

### Optional
- **Maven 3.2+** (if building Spark JAR locally)
- **Java 11+** (if building Spark JAR locally)

## 🚀 Quick Start

### One-Command Setup

The easiest way to get started is with our one-click setup script:

```bash
cd deploy
./setup.sh
```

Or using Make:

```bash
cd deploy
make setup
```

This single command will:
- ✅ Install prerequisites (Docker, Make, AWS CLI, etc.) if needed
- ✅ Create `.env` file from template (`.env.example`)
- ✅ Prompt you to fill in AWS credentials
- ✅ Start all services (Vector, Kafka, Spark, Grafana, Orchestrator, MySQL)
- ✅ Wait for services to become healthy
- ✅ Create Kafka topics automatically

**That's it!** Your LogWise stack will be up and running.

### Manual Setup (Advanced)

If you prefer to set up manually or need to customize the configuration:

#### 1. Install Prerequisites

```bash
# Run the bootstrap script to install prerequisites
./start.sh

# Or use Make
make bootstrap
```

#### 2. Configure Environment

Copy the example environment file and fill in your AWS credentials:

```bash
cp .env.example .env
# Edit .env with your AWS credentials, S3 bucket, Athena settings, etc.
```

See [Configuration Details](#-configuration-details) below for all available options.

#### 3. Start Services

```bash
make up
```

This will:
- Build custom Spark 3.1.2 Docker image
- Start all services (Vector, Kafka, Spark, Grafana, Orchestrator, MySQL)
- Wait for health checks to pass

#### 4. Create Kafka Topic

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
TENANT_VALUE=D11-Prod-AWS                  # Tenant identifier
```

For a complete list of all environment variables, see `.env.example` in the deploy directory.

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
make spark-run-jar MAIN_CLASS=com.logwise.spark.MainApplication APP_JAR=your-app.jar
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
aws s3 ls s3://${S3_BUCKET_NAME}/${S3_PREFIX}/
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
