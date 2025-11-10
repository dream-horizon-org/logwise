#!/usr/bin/env bash
set -eu
# Don't use pipefail as it can cause issues with curl

# Config via env
SPARK_REST_URL=${SPARK_REST_URL:-http://spark-master:6066/v1/submissions/create}
# Default to local JAR mounted into spark-master at /opt/app/app.jar
# If APP_RESOURCE is set to a host path, convert it to container path
APP_RESOURCE=${APP_RESOURCE:-file:/opt/app/app.jar}
# Normalize path: if it's a file:// path with a host path, convert to container path
if [[ "$APP_RESOURCE" == file:///* ]] && [[ "$APP_RESOURCE" != file:///opt/app/* ]]; then
  # If it's a host path (starts with /Users, /home, etc.), convert to container path
  if [[ "$APP_RESOURCE" =~ ^file:///(Users|home|mnt) ]]; then
    echo "Warning: Host path detected in APP_RESOURCE, converting to container path: $APP_RESOURCE -> file:/opt/app/app.jar" >&2
    APP_RESOURCE=file:/opt/app/app.jar
  fi
fi
# Also check if APP_RESOURCE is a plain path without file:// prefix and convert
if [[ "$APP_RESOURCE" == /* ]] && [[ "$APP_RESOURCE" != /opt/app/* ]]; then
  if [[ "$APP_RESOURCE" =~ ^/(Users|home|mnt) ]]; then
    echo "Warning: Host path detected in APP_RESOURCE, converting to container path: $APP_RESOURCE -> file:/opt/app/app.jar" >&2
    APP_RESOURCE=file:/opt/app/app.jar
  fi
fi
# Ensure file:// prefix if not present and it's a local path
if [[ "$APP_RESOURCE" == /opt/app/* ]] && [[ "$APP_RESOURCE" != file:* ]]; then
  APP_RESOURCE="file:$APP_RESOURCE"
fi
MAIN_CLASS=${MAIN_CLASS:-}
APP_ARGS=${APP_ARGS:-}
TENANT_HEADER=${TENANT_HEADER:-X-Tenant-Name}
TENANT_VALUE=${TENANT_VALUE:-D11-Prod-AWS}

# AWS credentials for S3 access
AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID:-}
AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY:-}
AWS_SESSION_TOKEN=${AWS_SESSION_TOKEN:-}
AWS_REGION=${AWS_REGION:-us-east-1}

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
CLIENT_SPARK_VERSION=${CLIENT_SPARK_VERSION:-3.1.2}

if [[ -z "$APP_RESOURCE" || -z "$MAIN_CLASS" ]]; then
  echo "APP_RESOURCE and MAIN_CLASS are required for REST submission" >&2
  exit 2
fi

# Build JSON arrays safely
# Each argument should be valid HOCON format (e.g., key=value or key="value with spaces/colons")
# Note: We need to split by comma but respect quoted strings
ARGS_JSON="[]"
if [[ -n "$APP_ARGS" ]]; then
  # Use a more sophisticated splitting that respects quoted strings
  # Split by comma, but only when not inside quotes
  ARR=()
  CURRENT=""
  IN_QUOTES=0
  ESCAPED=0
  
  for (( i=0; i<${#APP_ARGS}; i++ )); do
    char="${APP_ARGS:$i:1}"
    
    if [[ $ESCAPED -eq 1 ]]; then
      CURRENT+="$char"
      ESCAPED=0
      continue
    fi
    
    case "$char" in
      '\\')
        ESCAPED=1
        CURRENT+="$char"
        ;;
      '"')
        if [[ $IN_QUOTES -eq 0 ]]; then
          IN_QUOTES=1
        else
          IN_QUOTES=0
        fi
        CURRENT+="$char"
        ;;
      ',')
        if [[ $IN_QUOTES -eq 0 ]]; then
          # Split here
          CURRENT="${CURRENT## }"
          CURRENT="${CURRENT%% }"
          if [[ -n "$CURRENT" ]]; then
            ARR+=("$CURRENT")
          fi
          CURRENT=""
        else
          CURRENT+="$char"
        fi
        ;;
      *)
        CURRENT+="$char"
        ;;
    esac
  done
  
  # Add the last argument
  if [[ -n "$CURRENT" ]]; then
    CURRENT="${CURRENT## }"
    CURRENT="${CURRENT%% }"
    if [[ -n "$CURRENT" ]]; then
      ARR+=("$CURRENT")
    fi
  fi
  
  # Build JSON array
  FIRST=1
  ARGS_JSON="["
  for a in "${ARR[@]}"; do
    # Escape quotes and backslashes for JSON (but preserve the HOCON structure)
    a_escaped=$(printf '%s' "$a" | sed 's/\\/\\\\/g' | sed 's/"/\\"/g')
    if [[ $FIRST -eq 1 ]]; then
      ARGS_JSON="$ARGS_JSON\"$a_escaped\""
      FIRST=0
    else
      ARGS_JSON="$ARGS_JSON,\"$a_escaped\""
    fi
  done
  ARGS_JSON="$ARGS_JSON]"
fi

# Build environment variables JSON object
# Escape quotes and backslashes in values for JSON
escape_json() {
  printf '%s' "$1" | sed 's/\\/\\\\/g' | sed 's/"/\\"/g'
}

ENV_VARS_JSON="\"SPARK_ENV_LOADED\": \"1\""
ENV_VARS_JSON="${ENV_VARS_JSON}, \"${TENANT_HEADER}\": \"$(escape_json "${TENANT_VALUE}")\""

# Add AWS credentials if provided
if [[ -n "${AWS_ACCESS_KEY_ID}" ]]; then
  ENV_VARS_JSON="${ENV_VARS_JSON}, \"AWS_ACCESS_KEY_ID\": \"$(escape_json "${AWS_ACCESS_KEY_ID}")\""
fi
if [[ -n "${AWS_SECRET_ACCESS_KEY}" ]]; then
  ENV_VARS_JSON="${ENV_VARS_JSON}, \"AWS_SECRET_ACCESS_KEY\": \"$(escape_json "${AWS_SECRET_ACCESS_KEY}")\""
fi
if [[ -n "${AWS_SESSION_TOKEN}" ]]; then
  ENV_VARS_JSON="${ENV_VARS_JSON}, \"AWS_SESSION_TOKEN\": \"$(escape_json "${AWS_SESSION_TOKEN}")\""
fi
if [[ -n "${AWS_REGION}" ]]; then
  ENV_VARS_JSON="${ENV_VARS_JSON}, \"AWS_REGION\": \"$(escape_json "${AWS_REGION}")\""
fi

# Determine S3A endpoint based on region
# For us-east-1, use s3.amazonaws.com (no region prefix)
# For other regions, use s3-<region>.amazonaws.com
AWS_REGION=${AWS_REGION:-us-east-1}
if [[ "${AWS_REGION}" == "us-east-1" ]]; then
  S3A_ENDPOINT="s3.amazonaws.com"
else
  S3A_ENDPOINT="s3-${AWS_REGION}.amazonaws.com"
fi

# Determine credentials provider based on whether session token is present
# Use explicit credentials via S3A properties so executors don't rely on env propagation
if [[ -n "${AWS_SESSION_TOKEN}" ]]; then
  S3A_CREDENTIALS_PROVIDER="org.apache.hadoop.fs.s3a.TemporaryAWSCredentialsProvider"
  S3A_PROPERTIES_JSON='"spark.hadoop.fs.s3a.impl": "org.apache.hadoop.fs.s3a.S3AFileSystem",
    "spark.hadoop.fs.s3a.path.style.access": "false",
    "spark.hadoop.fs.s3a.aws.credentials.provider": "'"${S3A_CREDENTIALS_PROVIDER}"'",
    "spark.hadoop.fs.s3a.access.key": "'"$(escape_json "${AWS_ACCESS_KEY_ID:-}")"'",
    "spark.hadoop.fs.s3a.secret.key": "'"$(escape_json "${AWS_SECRET_ACCESS_KEY:-}")"'",
    "spark.hadoop.fs.s3a.session.token": "'"$(escape_json "${AWS_SESSION_TOKEN:-}")"'",
    "spark.hadoop.fs.s3a.endpoint": "'"${S3A_ENDPOINT}"'",
    "spark.driverEnv.AWS_ACCESS_KEY_ID": "'"$(escape_json "${AWS_ACCESS_KEY_ID:-}")"'",
    "spark.driverEnv.AWS_SECRET_ACCESS_KEY": "'"$(escape_json "${AWS_SECRET_ACCESS_KEY:-}")"'",
    "spark.driverEnv.AWS_SESSION_TOKEN": "'"$(escape_json "${AWS_SESSION_TOKEN:-}")"'",
    "spark.driverEnv.AWS_REGION": "'"$(escape_json "${AWS_REGION:-us-east-1}")"'",
    "spark.executorEnv.AWS_ACCESS_KEY_ID": "'"$(escape_json "${AWS_ACCESS_KEY_ID:-}")"'",
    "spark.executorEnv.AWS_SECRET_ACCESS_KEY": "'"$(escape_json "${AWS_SECRET_ACCESS_KEY:-}")"'",
    "spark.executorEnv.AWS_SESSION_TOKEN": "'"$(escape_json "${AWS_SESSION_TOKEN:-}")"'",
    "spark.executorEnv.AWS_REGION": "'"$(escape_json "${AWS_REGION:-us-east-1}")"'"'
else
  S3A_CREDENTIALS_PROVIDER="org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider"
  S3A_PROPERTIES_JSON='"spark.hadoop.fs.s3a.impl": "org.apache.hadoop.fs.s3a.S3AFileSystem",
    "spark.hadoop.fs.s3a.path.style.access": "false",
    "spark.hadoop.fs.s3a.aws.credentials.provider": "'"${S3A_CREDENTIALS_PROVIDER}"'",
    "spark.hadoop.fs.s3a.access.key": "'"$(escape_json "${AWS_ACCESS_KEY_ID:-}")"'",
    "spark.hadoop.fs.s3a.secret.key": "'"$(escape_json "${AWS_SECRET_ACCESS_KEY:-}")"'",
    "spark.hadoop.fs.s3a.endpoint": "'"${S3A_ENDPOINT}"'",
    "spark.driverEnv.AWS_ACCESS_KEY_ID": "'"$(escape_json "${AWS_ACCESS_KEY_ID:-}")"'",
    "spark.driverEnv.AWS_SECRET_ACCESS_KEY": "'"$(escape_json "${AWS_SECRET_ACCESS_KEY:-}")"'",
    "spark.driverEnv.AWS_REGION": "'"$(escape_json "${AWS_REGION:-us-east-1}")"'",
    "spark.executorEnv.AWS_ACCESS_KEY_ID": "'"$(escape_json "${AWS_ACCESS_KEY_ID:-}")"'",
    "spark.executorEnv.AWS_SECRET_ACCESS_KEY": "'"$(escape_json "${AWS_SECRET_ACCESS_KEY:-}")"'",
    "spark.executorEnv.AWS_REGION": "'"$(escape_json "${AWS_REGION:-us-east-1}")"'"'
fi

# Build JSON body - use printf instead of heredoc to avoid issues with set -eu
BODY=$(printf '{
  "action": "CreateSubmissionRequest",
  "appArgs": %s,
  "appResource": "%s",
  "clientSparkVersion": "%s",
  "mainClass": "%s",
  "environmentVariables": {
    %s
  },
  "sparkProperties": {
    "spark.app.name": "%s",
    "spark.cores.max": "%s",
    "spark.driver.cores": "%s",
    "spark.driver.extraJavaOptions": "%s",
    "spark.driver.maxResultSize": "%s",
    "spark.driver.memory": "%s",
    "spark.driver.supervise": %s,
    "spark.executor.cores": "%s",
    "spark.executor.extraJavaOptions": "%s",
    "spark.executor.memory": "%s",
    "spark.jars": "%s",
    "spark.master": "%s",
    "spark.submit.deployMode": "%s",
    %s
  }
}' \
  "${ARGS_JSON}" \
  "${APP_RESOURCE}" \
  "${CLIENT_SPARK_VERSION}" \
  "${MAIN_CLASS}" \
  "${ENV_VARS_JSON}" \
  "${SPARK_APP_NAME}" \
  "${SPARK_CORES_MAX}" \
  "${SPARK_DRIVER_CORES}" \
  "${SPARK_DRIVER_OPTS}" \
  "${SPARK_DRIVER_MAX_RESULT_SIZE:-2G}" \
  "${SPARK_DRIVER_MEMORY}" \
  "${SPARK_DRIVER_SUPERVISE}" \
  "${SPARK_EXECUTOR_CORES}" \
  "${SPARK_EXECUTOR_OPTS}" \
  "${SPARK_EXECUTOR_MEMORY}" \
  "${SPARK_JARS}" \
  "${SPARK_MASTER_URL}" \
  "${SPARK_DEPLOY_MODE}" \
  "${S3A_PROPERTIES_JSON}")

echo "Submitting via REST to ${SPARK_REST_URL}"
echo "Request body (first 500 chars): ${BODY:0:500}..." >&2

# Wait for Spark Master REST API to be ready
echo "Waiting for Spark Master REST API to be ready..."
for i in {1..30}; do
  # Check if REST API endpoint is reachable (any response is OK, even error responses mean it's up)
  if curl -s "${SPARK_REST_URL%/*}/status" >/dev/null 2>&1; then
    echo "Spark Master REST API is ready"
    break
  fi
  if [ $i -eq 30 ]; then
    echo "ERROR: Spark Master REST API not available after 30 attempts" >&2
    exit 1
  fi
  echo "Waiting... (attempt $i/30)"
  sleep 2
done

# Submit the job
echo "Submitting job..."
RESPONSE=$(curl -s -w "\n%{http_code}" -H 'Cache-Control: no-cache' -H 'Content-Type: application/json;charset=UTF-8' \
  --data "$BODY" "$SPARK_REST_URL" 2>&1) || {
  EXIT_CODE=$?
  echo "ERROR: curl failed with exit code: $EXIT_CODE" >&2
  echo "Response: $RESPONSE" >&2
  exit $EXIT_CODE
}

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY_RESPONSE=$(echo "$RESPONSE" | sed '$d')

echo "HTTP Status Code: $HTTP_CODE"
echo "Response: $BODY_RESPONSE"

if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
  echo "REST submission successful"
  exit 0
else
  echo "ERROR: REST submission failed with HTTP code: $HTTP_CODE" >&2
  echo "Response body: $BODY_RESPONSE" >&2
  exit 1
fi


