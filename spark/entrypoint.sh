#!/bin/bash
set -eu

# Enable error handling
exec > >(tee -a /proc/1/fd/1) 2>&1

echo "Starting spark-client entrypoint..."
echo "AUTO_SPARK_REST_SUBMIT=${AUTO_SPARK_REST_SUBMIT:-true}"
echo "MAIN_CLASS=${MAIN_CLASS:-not set}"
echo "APP_RESOURCE=${APP_RESOURCE:-not set}"

# If AUTO_SPARK_REST_SUBMIT is true, run rest-submit.sh
if [ "${AUTO_SPARK_REST_SUBMIT:-true}" = "true" ]; then
  echo "Running REST submission..."
  echo "Checking if /opt/rest-submit.sh exists..."
  ls -la /opt/rest-submit.sh || echo "ERROR: /opt/rest-submit.sh not found!" >&2
  echo "Executing /opt/rest-submit.sh..."
  if bash -x /opt/rest-submit.sh 2>&1; then
    echo "REST submission completed successfully"
    # Keep container running after submission
    sleep infinity
  else
    EXIT_CODE=$?
    echo "REST submission failed with exit code $EXIT_CODE" >&2
    exit 1
  fi
# Otherwise, sleep forever
else
  echo "AUTO_SPARK_REST_SUBMIT is false, sleeping..."
  sleep infinity
fi
