#!/usr/bin/env bash

set -e

echo "APP_DIR: ${APP_DIR}"
echo "ENV: ${ENV}"
echo "VPC_SUFFIX: ${VPC_SUFFIX}"
echo "TEAM_SUFFIX: ${TEAM_SUFFIX}"
echo "SERVICE_NAME: ${SERVICE_NAME}"

RESOURCES_PATH="./resources/"
bash ./.odin/fetch-config.sh
source .config
source ./.odin/.env

mvn com.dream11:config-maven-plugin:1.3.2:init


runMigrationsCmd="mvn com.dream11.migrations:migrations-maven-plugin:2.5.0:bootstrap -Dapp.environment=${ENV} -Dd11.resources.path=${RESOURCES_PATH}; mvn com.dream11.migrations:migrations-maven-plugin:2.5.0:up -Dapp.environment=${ENV} -Dd11.resources.path=${RESOURCES_PATH};"

# For production like environments only check if migrations are already run. We intend to run migrations manually or via mydbops
if [[ "$ENV" == prod* ]] || [[ "$ENV" == uat* ]]; then
  migrationsCmd="echo 'Not using migrations in production'"
else
  migrationsCmd=$runMigrationsCmd
fi

(eval "${migrationsCmd}")
