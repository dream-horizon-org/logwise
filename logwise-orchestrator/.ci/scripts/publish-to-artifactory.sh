#!/usr/bin/env bash
set -eo pipefail

cp -r .odin target/${ARTIFACT_NAME}
cd target
ZIP_NAME=${ARTIFACT_NAME}-${VERSION}.zip
zip -q -r ${ZIP_NAME} ${ARTIFACT_NAME}

jf rt upload --flat --url ${JFROG_URL} --user ${USERNAME} --password ${PASSWORD} \
${ZIP_NAME} ${JFROG_REPOSITORY}/${ARTIFACT_NAME}/${VERSION}/