#!/bin/bash
set -ev

if [ "${TRAVIS_REPO_SLUG}" != "pmd/pmd-designer" ] || [ "${TRAVIS_PULL_REQUEST}" != "false" ] || [ "${TRAVIS_SECURE_ENV_VARS}" != "true" ]; then
    echo "Not setting up secrets (TRAVIS_REPO_SLUG=${TRAVIS_REPO_SLUG} TRAVIS_PULL_REQUEST=${TRAVIS_PULL_REQUEST} TRAVIS_SECURE_ENV_VARS=${TRAVIS_SECURE_ENV_VARS})."
    exit 0
fi

# encrypted via "travis encrypt-file"
openssl aes-256-cbc -K $encrypted_f8e4782b4fdb_key -iv $encrypted_f8e4782b4fdb_iv -in .travis/pmd-designer-release-signing-key-0x8C2E4C5B-pub-sec.asc.enc -out .travis/pmd-designer-release-signing-key-0x8C2E4C5B-pub-sec.asc -d

mkdir -p "$HOME/.gpg"
gpg --batch --import .travis/pmd-designer-release-signing-key-0x8C2E4C5B-pub-sec.asc
rm .travis/pmd-designer-release-signing-key-0x8C2E4C5B-pub-sec.asc
