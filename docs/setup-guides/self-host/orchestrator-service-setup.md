---
title: Logwise Orchestrator (Self-Host)
---

# Logwise Orchestrator — Self-Hosted Setup

Follow these steps to run Logwise Orchestrator as a standalone self-hosted application using the JAR file.

## Prerequisites

- **Java 11** or higher (JRE or JDK) installed
- **Maven 3.6+** installed (for building the application)
- **MySQL 8.0+** running and accessible
- Access to required external services (AWS, GCP, Kafka, etc.) based on your configuration

## 1) Setup Database

Create the MySQL database and initialize schema:

```bash
mysql -u root -p
```

```sql
CREATE DATABASE IF NOT EXISTS log_central CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'logcentral'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON log_central.* TO 'logcentral'@'localhost';
FLUSH PRIVILEGES;
```

Initialize the database schema by running the following SQL script:

```sql
USE log_central;

DROP TABLE IF EXISTS service_details;
CREATE TABLE `service_details` (
  `environmentName` varchar(128) NOT NULL,
  `componentType` varchar(50) NOT NULL,
  `serviceName` varchar(50) NOT NULL,
  `retentionDays` mediumint unsigned NOT NULL,
  `tenant` enum('ABC') NOT NULL,
  `lastCheckedAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `environmentName` (`environmentName`,`componentType`,`serviceName`,`tenant`)
)
```

## 2) Build the Application

Navigate to the project directory and build the fat JAR:

```bash
cd logwise/orchestrator
mvn clean package
```

The fat JAR will be created at `target/log-central-orchestrator-<version>-all.jar`.

For example: `target/log-central-orchestrator-1.4.2-SNAPSHOT-all.jar`

Verify the JAR exists:

```bash
ls -lh target/log-central-orchestrator-*-all.jar
```

## 3) Configure Environment Variables

Set the required environment variables. Create a `.env` file or export them in your shell:

```bash
# Database Configuration (Required)
export DB_MASTER_HOST=localhost
export DB_USERNAME=logcentral
export DB_PASSWORD=your_secure_password
export DB_SLAVE_HOST=localhost  # Optional, same as master if not using replication

# Application Environment (Optional - defaults to "dev" if not set)
# Set as system property: -Dapp.environment=local
# Or export: export app.environment=local

# Encryption Configuration (Required)
export ENCRYPTION_KEY=your_encryption_key
export ENCRYPTION_IV=your_encryption_iv

# Kafka Configuration (Required if using Kafka features)
export D11_PROD_AWS_KAFKA_MANAGER_URL=your_kafka_manager_url
export D11_PROD_AWS_KAFKA_BROKERS_HOST=your_kafka_brokers
export D11_PROD_AWS_ASG_KAFKA_BROKERS_HOST=your_asg_kafka_brokers  # Optional

# Spark Configuration (Required if using Spark features)
export D11_PROD_AWS_SPARK_MASTER_HOST=your_spark_master_host
export D11_PROD_AWS_SPARK_JAR_PATH=your_spark_jar_path
export D11_PROD_AWS_LOG4J_PROPERTIES_FILE_PATH=your_log4j_path

# AWS Configuration (Required if using AWS services like S3, Athena)
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key

# S3 Endpoint Override
export S3_ENDPOINT_OVERRIDE=http://localhost:4566
```

If using a `.env` file, source it:

```bash
source .env
```

::: warning Important
The database name is fixed as `log_central`. Ensure your MySQL database is created with this exact name. The application will connect to this database using the credentials specified in `DB_USERNAME` and `DB_PASSWORD`.
:::

## 4) Run the Application

Run the JAR file directly:

```bash
java -jar target/log-central-orchestrator-<version>-all.jar
```

For example:

```bash
java -jar target/log-central-orchestrator-1.4.2-SNAPSHOT-all.jar
```

To specify the application environment explicitly:

```bash
java -Dapp.environment=local -jar target/log-central-orchestrator-<version>-all.jar
```

::: note Note
If `app.environment` is not set, it defaults to `"dev"`. The application will look for configuration files matching the environment name (e.g., `application-local.conf`, `connection-local.conf`).
:::


::: warning Important

To change the default port, set the system property: `-Dhttp.default.port=<port>` when running the JAR.
:::

## Verifying the Application

1. **Check if the application is running:**
   ```bash
   ps aux | grep log-central-orchestrator
   ```

2. **Test the health check endpoint:**
   ```bash
   curl http://localhost:8080/healthcheck
   ```


## Troubleshooting

### Database Connection Failed
- Verify MySQL is running: `systemctl status mysql` or `mysqladmin ping`
- Check database credentials in environment variables (`DB_USERNAME`, `DB_PASSWORD`, `DB_MASTER_HOST`)
- Verify database exists: `mysql -u root -p -e "SHOW DATABASES;"` (should show `log_central`)
- Ensure database name is exactly `log_central` (case-sensitive)
- Test connection manually: `mysql -u logcentral -p -h localhost log_central`

### JAR File Not Found
- Verify the JAR file exists: `ls -lh target/*-all.jar`
- Ensure you're using the `-all.jar` file (fat JAR), not the regular JAR
- Rebuild if necessary: `mvn clean package`
