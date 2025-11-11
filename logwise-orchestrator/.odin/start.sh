#!/usr/bin/env bash
set -e
set -x

echo "APP_DIR: ${APP_DIR}"
echo "ENV: ${ENV}"
echo "VPC_SUFFIX: ${VPC_SUFFIX}"
echo "TEAM_SUFFIX: ${TEAM_SUFFIX}"
echo "SERVICE_NAME: ${SERVICE_NAME}"
echo "NAMESPACE: ${NAMESPACE}"
echo "TARGET_GROUP_ARN: ${TARGET_GROUP_ARN}"

# ===== Variables =====

OTEL_VERSION="0.133.0"
OTEL_TARBALL="otelcol-contrib_${OTEL_VERSION}_linux_amd64.tar.gz"
OTEL_URL="https://github.com/open-telemetry/opentelemetry-collector-releases/releases/download/v${OTEL_VERSION}/${OTEL_TARBALL}"
INSTALL_DIR="/opt/otelcol-contrib"
CONFIG_DIR="/etc/otelcol-contrib"
BIN_PATH="${INSTALL_DIR}/otelcol-contrib"

# ===== OpenTelemetry Setup =====
mkdir -p ${INSTALL_DIR}
wget -q ${OTEL_URL} -O /tmp/${OTEL_TARBALL}
tar -xzf /tmp/${OTEL_TARBALL} -C ${INSTALL_DIR}
rm /tmp/${OTEL_TARBALL}
# Install Java agent

JAVA_OTEL_URL="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar"
wget -q ${JAVA_OTEL_URL} -O ${INSTALL_DIR}/opentelemetry-javaagent.jar

# ===== Config directory =====

mkdir -p ${CONFIG_DIR}
S3_BUCKET="signoz-otel"
aws s3 cp "s3://${S3_BUCKET}/otel_collector.yaml" ${CONFIG_DIR}/otelcol.yaml

# ===== Systemd unit =====

cat >/etc/systemd/system/otelcol-contrib.service <<EOF
[Unit]
Description=OpenTelemetry Collector Contrib
After=network-online.target
Wants=network-online.target
[Service]
ExecStart=${BIN_PATH} --config ${CONFIG_DIR}/otelcol.yaml
Restasrt=always
RestartSec=5
User=root
LimitNOFILE=65535
[Install]
WantedBy=multi-user.target
EOF

# Enable & start otelcol service
systemctl daemon-reload
systemctl enable --now otelcol-contrib
export D11_LOGGING_GCP_CREDENTIALS_PATH=${D11_LOGGING_GCP_CREDENTIALS_PATH:-/opt/keys/d11-logging-736111854891-compute-key.json}
aws s3 cp s3://central-log-management-central-prod/keys/d11-logging-736111854891-compute-key.json $D11_LOGGING_GCP_CREDENTIALS_PATH

echo "Starting With Vector Setup..."
PARENT_DIR=$(dirname "${BASH_SOURCE[0]}")
aws s3 cp s3://central-log-management-central-prod/vector/rpm_vector_0.32.2-1_amd64.deb .
dpkg -i rpm_vector_0.32.2-1_amd64.deb
cp "${PARENT_DIR}"/vector.toml /etc/vector/vector.toml

OTEL_RESOURCE_ATTRIBUTES="service_name=${SERVICE_NAME},component_name=${COMPONENT_NAME},artifact_name=${ARTIFACT_NAME},team=${TEAM_SUFFIX},vpc=${VPC_SUFFIX},env=${ENV},namespace=${NAMESPACE}"
OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4318"
JAVA_OPTS="-XX:+HeapDumpOnOutOfMemoryError"
OTEL_OPTS="-javaagent:${INSTALL_DIR}/opentelemetry-javaagent.jar -Dotel.exporter.otlp.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT} -Dotel.resource.attributes=${OTEL_RESOURCE_ATTRIBUTES}"JMX_OPTS="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
JMX_OPTS="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
APP_OPTS="-Dapp.environment=${ENV} -Dvertx.disableDnsResolver=true -DIGNITE_JETTY_PORT=9000 --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED --add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-exports=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED --add-exports=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED --add-exports=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED --illegal-access=permit"
if [[ "${DEPLOYMENT_TYPE}" == "container" ]]; then
  LOG_OPTS="-Dlog.directory.path=/app/logs -Dlog.file.name=${SERVICE_NAME}.log -Dlogback.configurationFile=${APP_DIR}/resources/logback/logback-docker.xml"
  MEM_OPTS="-Xms512m -Xmx512m -Xmn256m"
else
  LOG_OPTS="-Dlogback.configurationFile=${APP_DIR}/resources/logback/logback.xml"
  totalMem=${TOTAL_MEMORY:-$(free -m | head -2 | tail -1 | awk '{print $2}')}
  heapSize=$((totalMem * 70 / 100))
  halfHeapSize=$((heapSize / 2))
  MEM_OPTS="-Xms${heapSize}m -Xmx${heapSize}m -Xmn${halfHeapSize}m"
fi

APP_JAR="${COMPONENT_NAME}-${ARTIFACT_VERSION}-fat.jar"
PATH_TO_APP_JAR="${APP_DIR}/${APP_JAR}"

cd "${APP_DIR}" || exit

if [[ "${DEPLOYMENT_TYPE}" == "container" ]]; then
  TEAM_SUFFIX=${TEAM_SUFFIX} VPC_SUFFIX=${VPC_SUFFIX} java -jar ${JAVA_OPTS} ${MEM_OPTS} ${OTEL_OPTS} \
  ${JMX_OPTS} ${APP_OPTS} ${LOG_OPTS} ${PATH_TO_APP_JAR}
else
#  kill -9 "$(jps | grep $COMPONENT_NAME | awk '{print $1}')"
  PID=$(jps | grep $COMPONENT_NAME | awk '{print $1}')
  if [ -n "$PID" ]; then
      kill -9 "$PID"
      echo "Process $PID killed."
  else
      echo "No process found for $APP_JAR."
  fi
  nohup java -jar ${JAVA_OPTS} ${MEM_OPTS} ${OTEL_OPTS} ${JMX_OPTS} ${APP_OPTS} ${LOG_OPTS} ${PATH_TO_APP_JAR} </dev/null >/dev/null 2>&1 &
  pid=$!
  [ -n "$PID_PATH" ] && echo $pid >"${PID_PATH}"
fi
