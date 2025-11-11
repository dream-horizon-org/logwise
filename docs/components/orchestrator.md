# Orchestrator

The Orchestrator Service acts as the control plane for the Logwise system, managing critical operations including metadata synchronization, log sync delay tracking, and Spark job monitoring. Currently supports AWS for component sync and delay metrics.

## Architecture in Logwise

```
Vector → Kafka → Spark Jobs → Object Storage (S3) 
                    ↑                    ↑                      
                    |                    |                      
    Orchestrator Service (Manages Discovery, Sync & Monitoring)
```

The Orchestrator Service enables:

- **Metadata Synchronization** - Syncs service metadata between object storage and database
- **Log Sync Delay Tracking** - Monitors delay between log generation and storage
- **Spark Job Monitoring** - Monitors and auto-submits Spark jobs for log processing

## Key Features

### 1. Metadata Management
- **Service Discovery**: Automatically discovers services from object storage partitions (AWS only)
- **Database Sync**: Syncs service metadata between S3 and database
- **Auto-Onboarding**: Automatically onboards new services to database
- **Cleanup**: Removes services that no longer exist in object storage

### 2. Spark Job Monitoring
- **Auto-Monitoring**: Monitors Spark driver status periodically
- **Auto-Submission**: Automatically submits Spark jobs when driver is not running
- **State Management**: Handles Spark state cleanup and recovery scenarios
- **Timestamp-based Processing**: Supports submitting jobs with specific Kafka offsets for historical processing
- **Fault Tolerance**: Automatically recovers from Spark driver failures

### 3. Log Sync Delay Metrics
- **Delay Calculation**: Computes delay between log generation and storage in S3
- **AWS Only**: Currently supports AWS (S3) for delay calculation
- **Application Logs**: Tracks delay for application logs only
- **Real-time Monitoring**: Provides real-time delay metrics via REST API

## Metadata Management

### Service Discovery

The Orchestrator automatically discovers services by scanning object storage partitions. Services are identified by their path structure:

**Format**: `logs/env={env}/service_name={service}/component_name={component}/`

**Example**: `logs/env=prod/service_name=api-service/component_name=api-container/`

**Process**:
1. Scans S3 prefixes to discover all unique service combinations
2. Compares discovered services with database entries
3. Onboards new services to database
4. Removes services that no longer exist in object storage

**Supported Platforms**:
- **AWS**: Discovers services from S3 partitions (only AWS is currently supported)

## Spark Job Monitoring

### Monitoring Process

The Orchestrator monitors Spark jobs through API calls:

**Monitoring Cycle**:
- Polls Spark Master API every 15 seconds
- Checks driver status (RUNNING, FINISHED, FAILED, etc.)
- Monitors for up to 60 seconds (4 polls total)
- Stops monitoring once job is submitted

**Decision Logic**:
```java
if (driver is not running) {
    submit job immediately
} else if (pending timestamp-based submission exists) {
    clean Spark state
    submit job with specific timestamp
} else if (resume to subscribe pattern needed) {
    clean Spark state
    submit job with resume configuration
}
```

### Spark Job Submission

**Configuration**:
- **Kafka Configuration**: Broker hosts, topic patterns, starting offsets (passed to Spark jobs - orchestrator doesn't manage Kafka directly)
- **Spark Resources**: Driver/executor cores, memory allocation
- **Processing Parameters**: Max rate per partition, subscribe patterns
- **Cloud Storage**: S3 paths for checkpoints and logs

**Job Lifecycle**:
1. Validates Spark Master connectivity
2. Cleans previous state (checkpoints, WAL files) if needed
3. Submits job to Spark Master REST API
4. Updates submission status in database

### Spark State Management

**State Cleanup**:
- Deletes checkpoint files from object storage
- Removes WAL (Write-Ahead Log) files
- Cleans Spark metadata files
- Required before timestamp-based job submissions

## Metrics & Monitoring

### Log Sync Delay Metrics

The Orchestrator computes and reports log processing delays:

**Application Logs Delay (AWS Only)**:
- Checks S3 object timestamps in partitioned paths
- Scans S3 prefixes for configured service logs 
- Extracts timestamp from object path: `hour={HH}/minute={MM}/`
- Calculates delay in minutes: `currentTime - latestLogTime`
- Searches up to 3 hours back (MAX_LOGS_SYNC_DELAY_HOURS)

**Metrics Response**:
- Returns `LogSyncDelayResponse` with:
  - `tenant`: Tenant name
  - `appLogsDelayMinutes`: Delay in minutes for application logs
- Returns max delay (180 minutes) when no logs found (indicates potential issues)

## API Endpoints

### Component Management
- `POST /api/v1/component/sync` - Sync application services (AWS only)
  - **Headers**: `X-Tenant-Name` (required)
  - **Request Body**: `ComponentSyncRequest` with `componentType` (must be "application")
  - **Response**: `DefaultSuccessResponse` with sync status message

### Spark Management
- `POST /monitor-spark-job` - Monitor and submit Spark jobs
  - **Headers**: `X-Tenant-Name` (required)
  - **Request Body**: `MonitorSparkJobRequest` (optional driverCores, driverMemoryInGb)
  - **Response**: `DefaultSuccessResponse` with success message

### Metrics
- `GET /api/v1/metric/sync-delay` - Get log sync delay metrics (AWS only)
  - **Headers**: `X-Tenant-Name` (required)
  - **Response**: `LogSyncDelayResponse` with:
    - `tenant`: Tenant name
    - `appLogsDelayMinutes`: Delay in minutes (null if error)

### Health Check
- `GET /healthcheck` - Application health status
  - **Response**: Health check status including MySQL connectivity

## Configuration

The Orchestrator uses HOCON configuration files with environment variable substitution:

**Key Configuration Sections**:
- **Tenants**: Multi-tenant configuration with isolated settings
- **Kafka**: Broker hosts, manager URLs, rate limits (used to configure Spark jobs - orchestrator doesn't manage Kafka)
- **Spark**: Master hosts, resource allocation, job configuration
- **Object Storage**: S3 buckets, regions, role ARNs
- **Delay Metrics**: Sample service configuration (env, serviceName, componentName) for delay calculation

**Environment Variables**:
- Database credentials
- AWS credentials
- Kafka broker hosts
- Spark master hosts

## Technology Stack

- **Framework**: Vert.x (reactive Java framework)
- **Language**: Java 11
- **Dependency Injection**: Google Guice
- **Database**: MySQL
- **Cloud Support**: AWS (S3) - primary support
- **Build Tool**: Maven
- **Reactive Programming**: RxJava

## Requirements and Setup

See the [Orchestrator Setup Guide](/setup-guides/self-host/orchestrator-service-setup).