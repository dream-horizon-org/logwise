#!/usr/bin/env bash
set -euo pipefail

# Config via env
SPARK_REST_URL=${SPARK_REST_URL:-http://spark-master:6066/v1/submissions/create}
# Default to local JAR mounted into spark-master at /opt/app/app.jar
APP_RESOURCE=${APP_RESOURCE:-file:/opt/app/app.jar}
MAIN_CLASS=${MAIN_CLASS:-}
APP_ARGS=${APP_ARGS:-}
TENANT_HEADER=${TENANT_HEADER:-X-Tenant-Name}
TENANT_VALUE=${TENANT_VALUE:-D11-Prod-AWS}

SPARK_MASTER_URL=${SPARK_MASTER_URL:-spark://spark-master:7077}
SPARK_APP_NAME=${SPARK_APP_NAME:-d11-log-management}
SPARK_CORES_MAX=${SPARK_CORES_MAX:-4}
SPARK_DRIVER_CORES=${SPARK_DRIVER_CORES:-1}
SPARK_DRIVER_MEMORY=${SPARK_DRIVER_MEMORY:-1G}
SPARK_EXECUTOR_CORES=${SPARK_EXECUTOR_CORES:-1}
SPARK_EXECUTOR_MEMORY=${SPARK_EXECUTOR_MEMORY:-1G}
SPARK_DEPLOY_MODE=${SPARK_DEPLOY_MODE:-cluster}
SPARK_DRIVER_SUPERVISE=${SPARK_DRIVER_SUPERVISE:-false}
SPARK_DRIVER_OPTS=${SPARK_DRIVER_OPTS:-}
SPARK_EXECUTOR_OPTS=${SPARK_EXECUTOR_OPTS:-}
SPARK_JARS=${SPARK_JARS:-$APP_RESOURCE}
CLIENT_SPARK_VERSION=${CLIENT_SPARK_VERSION:-3.5.1}

if [[ -z "$APP_RESOURCE" || -z "$MAIN_CLASS" ]]; then
  echo "APP_RESOURCE and MAIN_CLASS are required for REST submission" >&2
  exit 2
fi

# Build JSON arrays safely
ARGS_JSON="[]"
if [[ -n "$APP_ARGS" ]]; then
  IFS=',' read -ra ARR <<< "$APP_ARGS"
  FIRST=1
  ARGS_JSON="["
  for a in "${ARR[@]}"; do
    a="${a## }"; a="${a%% }"
    if [[ $FIRST -eq 1 ]]; then
      ARGS_JSON="$ARGS_JSON\"$a\""
      FIRST=0
    else
      ARGS_JSON="$ARGS_JSON,\"$a\""
    fi
  done
  ARGS_JSON="$ARGS_JSON]"
fi

read -r -d '' BODY <<JSON
{
  "action": "CreateSubmissionRequest",
  "appArgs": ${ARGS_JSON},
  "appResource": "${APP_RESOURCE}",
  "clientSparkVersion": "${CLIENT_SPARK_VERSION}",
  "mainClass": "${MAIN_CLASS}",
  "environmentVariables": {
    "SPARK_ENV_LOADED": "1",
    "${TENANT_HEADER}": "${TENANT_VALUE}"
  },
  "sparkProperties": {
    "spark.app.name": "${SPARK_APP_NAME}",
    "spark.cores.max": "${SPARK_CORES_MAX}",
    "spark.driver.cores": "${SPARK_DRIVER_CORES}",
    "spark.driver.extraJavaOptions": "${SPARK_DRIVER_OPTS}",
    "spark.driver.maxResultSize": "${SPARK_DRIVER_MAX_RESULT_SIZE:-2G}",
    "spark.driver.memory": "${SPARK_DRIVER_MEMORY}",
    "spark.driver.supervise": ${SPARK_DRIVER_SUPERVISE},
    "spark.executor.cores": "${SPARK_EXECUTOR_CORES}",
    "spark.executor.extraJavaOptions": "${SPARK_EXECUTOR_OPTS}",
    "spark.executor.memory": "${SPARK_EXECUTOR_MEMORY}",
    "spark.jars": "${SPARK_JARS}",
    "spark.master": "${SPARK_MASTER_URL}",
    "spark.submit.deployMode": "${SPARK_DEPLOY_MODE}"
  }
}
JSON

echo "Submitting via REST to ${SPARK_REST_URL}"
curl -fsS -H 'Cache-Control: no-cache' -H 'Content-Type: application/json;charset=UTF-8' \
  --data "$BODY" "$SPARK_REST_URL"


