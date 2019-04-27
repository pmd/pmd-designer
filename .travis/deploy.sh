#!/bin/bash
set -ev

VERSION=$(./mvnw -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive org.codehaus.mojo:exec-maven-plugin:1.5.0:exec)
echo "Building PMD Designer ${VERSION} on branch ${TRAVIS_BRANCH}"

# builds on forks
if [ "${TRAVIS_REPO_SLUG}" != "pmd/pmd-designer" ] || [ "${TRAVIS_PULL_REQUEST}" != "false" ] || [ "${TRAVIS_SECURE_ENV_VARS}" != "true" ]; then
    ./mvnw verify -B -V
    exit 0
fi

# builds on pmd/pmd-designer
if [[ "${VERSION}" != *-SNAPSHOT && "${TRAVIS_TAG}" != "" ]]; then
    # release build
    ./mvnw deploy -Prelease -B -V
elif [[ "${VERSION}" == *-SNAPSHOT ]]; then
    # snapshot build
    ./mvnw deploy -B -V
else
    # other build. Can happen during release: the commit with a non snapshot version is built, but not from the tag.
    ./mvnw verify -B -V
    # we stop here - no need to execute further steps
    exit 0
fi
