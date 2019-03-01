#!/bin/bash
set -ev

VERSION=$(./mvnw -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive org.codehaus.mojo:exec-maven-plugin:1.5.0:exec)
echo "Building PMD Designer ${VERSION} on branch ${TRAVIS_BRANCH}"

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
