#!/usr/bin/env bash
set -e

RELEASE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
mvn versions:set -DnextSnapshot=true
BUMP_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

git add pom.xml
git commit -m "chore: release version ${RELEASE_VERSION} and bump version to ${BUMP_VERSION}"
git push origin HEAD:main
