#!/bin/bash
set -ev

if [ "${TRAVIS_PULL_REQUEST}" != "false" ] || [ "${TRAVIS_SECURE_ENV_VARS}" != "true" ]; then
    echo "Not setting up secrets (TRAVIS_PULL_REQUEST=${TRAVIS_PULL_REQUEST} TRAVIS_SECURE_ENV_VARS=${TRAVIS_SECURE_ENV_VARS})."
    exit 0
fi


# TODO - setup via travis command line client:
#  travis login --com
#  travis encrypt-file .travis/pmd-designer-release-signing-key.sec
#
#openssl aes-256-cbc -K $encrypted_cb4f24b6413c_key -iv $encrypted_cb4f24b6413c_iv -in .travis/pmd-designer-release-signing-key.sec.enc -out .travis/pmd-designer-release-signing-key.sec -d

mkdir -p "$HOME/.gpg"
#gpg --batch --import .travis/pmd-designer-release-signing-key.sec
#rm .travis/pmd-designer-release-signing-key.sec
