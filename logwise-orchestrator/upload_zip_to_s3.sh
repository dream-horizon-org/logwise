#!/usr/bin/env bash
set -eo pipefail
set -x

BUCKET_NAME=central-log-management-central-prod
ARTIFACT_NAME=log-central-orchestrator

mvn -B clean package -Dmaven.test.skip

PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
if [[ ${PROJECT_VERSION} != *-SNAPSHOT ]]; then
  echo "Error: version must be suffixed with '-SNAPSHOT'. Concrete versions can only be released from main branch"
  exit 1
fi
echo "PROJECT_VERSION=$PROJECT_VERSION"

cd target
ZIP_NAME=${ARTIFACT_NAME}-${PROJECT_VERSION}.zip
zip -q -r ${ZIP_NAME} ${ARTIFACT_NAME}

aws s3 cp ${ZIP_NAME} s3://$BUCKET_NAME/${ARTIFACT_NAME}/${PROJECT_VERSION}/