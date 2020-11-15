#!/usr/bin/env bash

set -e

function build_designer() {
    echo "::group::Install OpenJDK"
    install_openjdk
    echo "::endgroup::"

    VERSION=$(./mvnw -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive org.codehaus.mojo:exec-maven-plugin:1.5.0:exec)
    echo "Building PMD Designer ${VERSION} on branch ${PMD_CI_GIT_REF} (${PMD_CI_REPO})"

    # builds on forks, builds for pull requests
    if [[ "${PMD_CI_REPO}" != "pmd/pmd-designer" || -n "${PMD_CI_PULL_REQUEST_NUMBER}" ]]; then
        ./mvnw clean verify -B -V -e
        exit 0
    fi


    # builds on pmd/pmd-designer
    echo "::group::Setup Tasks"
    setup_secrets
    setup_maven
    echo "::endgroup::"

    # snapshot or release - it only depends on the version (SNAPSHOT or no SNAPSHOT)
    ./mvnw clean deploy -B -V -e -Psign
}

## helper functions

function setup_secrets() {
    echo "Setting up secrets..."
    # Required secrets are: CI_DEPLOY_USERNAME, CI_DEPLOY_PASSWORD, CI_SIGN_KEYNAME, CI_SIGN_PASSPHRASE
    local -r env_file=".ci/files/env"
    printenv PMD_CI_SECRET_PASSPHRASE | gpg --batch --yes --decrypt \
        --passphrase-fd 0 \
        --output ${env_file} ${env_file}.gpg
    source ${env_file} >/dev/null 2>&1
    rm ${env_file}

    local -r key_file=".ci/files/release-signing-key-D0BF1D737C9A1C22.asc"
    printenv PMD_CI_SECRET_PASSPHRASE | gpg --batch --yes --decrypt \
        --passphrase-fd 0 \
        --output ${key_file} ${key_file}.gpg
    gpg --batch --import ${key_file}
    rm ${key_file}
}

function setup_maven() {
    echo "Setting up Maven..."
    mkdir -p ${HOME}/.m2
    cp .ci/files/maven-settings.xml ${HOME}/.m2/settings.xml
}

function install_openjdk() {
    echo "Installing OpenJDK"
    OPENJDK_VERSION=11
    JDK_OS=linux
    COMPONENTS_TO_STRIP=1 # e.g. openjdk-11.0.3+7/bin/java
    DOWNLOAD_URL=$(curl --silent -X GET "https://api.adoptopenjdk.net/v3/assets/feature_releases/${OPENJDK_VERSION}/ga?architecture=x64&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=${JDK_OS}&page=0&page_size=1&project=jdk&sort_method=DEFAULT&sort_order=DESC&vendor=adoptopenjdk" \
        -H "accept: application/json" \
        | jq -r ".[0].binaries[0].package.link")
    OPENJDK_ARCHIVE=$(basename ${DOWNLOAD_URL})
    CACHE_DIR=${HOME}/.cache/openjdk
    TARGET_DIR=${HOME}/openjdk${OPENJDK_VERSION}
    mkdir -p ${CACHE_DIR}
    mkdir -p ${TARGET_DIR}
    if [ ! -e ${CACHE_DIR}/${OPENJDK_ARCHIVE} ]; then
        echo "Downloading from ${DOWNLOAD_URL} to ${CACHE_DIR}"
        curl --location --output ${CACHE_DIR}/${OPENJDK_ARCHIVE} "${DOWNLOAD_URL}"
    else
        echo "Skipped download, file ${CACHE_DIR}/${OPENJDK_ARCHIVE} already exists"
    fi
    tar --extract --file ${CACHE_DIR}/${OPENJDK_ARCHIVE} -C ${TARGET_DIR} --strip-components=${COMPONENTS_TO_STRIP}
    export JAVA_HOME="${TARGET_DIR}"
    export PATH="${TARGET_DIR}/bin:${PATH}"
    java -version
    echo "Java is available at ${TARGET_DIR}"
}

build_designer
