# log-central-orchestrator

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- MySQL 8.0+

## Quick Start

### 1. Setup Database

```sql
CREATE DATABASE IF NOT EXISTS log_central CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'logcentral'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON log_central.* TO 'logcentral'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Build Application

```bash
mvn clean package
```

The fat JAR will be created at `target/log-central-orchestrator-<version>-all.jar`

### 3. Configure Environment

Set required environment variables:

```bash
export DB_MASTER_HOST=localhost
export DB_USERNAME=logcentral
export DB_PASSWORD=your_password
```

### 4. Run Application

```bash
java -Dapp.environment=local -jar target/log-central-orchestrator-<version>-all.jar
```

Health check: `curl http://localhost:8080/healthcheck`

## Running Tests
* Run `mvn clean verify`

## Code formatting

* Run `mvn com.spotify.fmt:fmt-maven-plugin:2.20:format` to auto format the code

