# Log Central - Local Deployment (Docker Compose)

This stack deploys Vector → Kafka (KRaft) → Spark (writes Parquet to S3/Athena), a Spring Boot Orchestrator with MySQL, Grafana (Athena datasource), and cron jobs.

## Prerequisites
- Docker + Docker Compose v2
- Make
- AWS credentials (access to target S3 bucket and Athena workgroup)

Quick bootstrap installs missing tools:

```bash
./start.sh   # or: make bootstrap
```

## Bring-up
1. Copy `.env.example` to `.env` and fill values (AWS creds, etc.).
2. Start services:
   ```bash
   make up
   ```
3. Create Kafka topic:
   ```bash
   make topics
   ```

## Spark submit
Run the on-demand Spark job (reads from Kafka, writes Parquet to S3):
```bash
make spark-submit
```
By default, a minimal PySpark job is generated in the container if none is provided.
- Streaming toggle: set `SPARK_STREAMING=true|false` in `.env`.
- Output path: `s3://${S3_BUCKET}/${S3_PREFIX}/`.
- Checkpoints: `/opt/checkpoints` (mapped to `spark_checkpoint` volume).

## Grafana
- URL: http://localhost:${GRAFANA_PORT}
- Login: `${GF_SECURITY_ADMIN_USER}` / `${GF_SECURITY_ADMIN_PASSWORD}`
- The Athena datasource is auto-provisioned. Ensure:
  - `AWS_REGION`, `S3_ATHENA_OUTPUT`, `ATHENA_WORKGROUP`, `ATHENA_CATALOG`, `ATHENA_DATABASE` are set in `.env`.
  - The `grafana-athena-datasource` plugin is installed via `GF_INSTALL_PLUGINS`.

## Orchestrator
- Health: `curl http://localhost:${ORCH_PORT}/actuator/health`
- App reads DB config from `orchestrator/application.yml` (mounted in the container).
- On startup, app should initialize DB/migrations (Spring Boot profile `container`).

## Cron jobs
A minimal `supercronic` container calls 5 Orchestrator endpoints:
```
*/5 * * * * curl -fsS http://orchestrator:8080/api/job1?token=${CRON_TOKEN}
10 * * * * curl -fsS http://orchestrator:8080/api/job2?token=${CRON_TOKEN}
30 2 * * * curl -fsS http://orchestrator:8080/api/job3?token=${CRON_TOKEN}
0 */6 * * * curl -fsS http://orchestrator:8080/api/job4?token=${CRON_TOKEN}
15 4 * * 1 curl -fsS http://orchestrator:8080/api/job5?token=${CRON_TOKEN}
```

## Smoke tests
1. Vector → Kafka
   - Write a sample log to `vector/demo/test.log` (JSON per the sample schema or any text):
     ```bash
     echo '{"level":"INFO","message":"hello","app":"demo","ts":"2025-01-01T00:00:00Z"}' >> vector/demo/test.log
     ```
   - Verify Kafka sees the topic list (healthcheck) and optionally consume with:
     ```bash
     docker compose exec -T kafka bash -lc "kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic $KAFKA_TOPIC --from-beginning --timeout-ms 2000 || true"
     ```
2. Spark → S3
   - Run `make spark-submit`
   - Verify Parquet at `s3://${S3_BUCKET}/${S3_PREFIX}/`
3. Athena → Grafana
   - Ensure an Athena table points at the S3 path above and that `${ATHENA_DATABASE}` is set.
   - Open Grafana → Explore → choose Athena → run a simple query (e.g. `SELECT * FROM your_table LIMIT 10`).
4. Orchestrator
   - `curl -f http://localhost:${ORCH_PORT}/actuator/health`

## Common issues
- Port conflicts: change `${GRAFANA_PORT}` or `${ORCH_PORT}` in `.env`.
- AWS creds: ensure `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` are set; region matches bucket.
- Grafana plugin missing: confirm `GF_INSTALL_PLUGINS=grafana-athena-datasource` and restart Grafana.
- Kafka DNS: services talk on the `lc_net` network; brokers should be `kafka:9092`.
- Spark jars: ensure `spark-sql-kafka-0-10`, `hadoop-aws`, `aws-java-sdk-bundle` downloaded in the Spark image.

## Swap to managed Kafka (prod note)
- Set `KAFKA_BROKERS` to the managed bootstrap servers; ensure network access.
- Remove internal Kafka service from Compose if desired; topic creation via provider tools.

## TLS (out of scope here)
- Terminate TLS at a load balancer/ingress in production; containers can run HTTP internally.

## Make targets
- `make bootstrap` – run `start.sh` to install prerequisites
- `make up` – start all services
- `make down` – stop services
- `make ps` – list services
- `make logs` – tail logs
- `make topics` – create Kafka topic
- `make spark-submit` – run Spark driver
- `make teardown` – stop and remove volumes


