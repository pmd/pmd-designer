#!/bin/bash
set -ev

if [ "${TRAVIS_REPO_SLUG}" != "pmd/pmd-designer" ] || [ "${TRAVIS_PULL_REQUEST}" != "false" ] || [ "${TRAVIS_SECURE_ENV_VARS}" != "true" ]; then
    echo "Not setting up secrets (TRAVIS_REPO_SLUG=${TRAVIS_REPO_SLUG} TRAVIS_PULL_REQUEST=${TRAVIS_PULL_REQUEST} TRAVIS_SECURE_ENV_VARS=${TRAVIS_SECURE_ENV_VARS})."
    exit 0
fi

# encrypted via "travis encrypt-file"
openssl aes-256-cbc -K $encrypted_a5724fade5c6_key -iv $encrypted_a5724fade5c6_iv -in .travis/release-signing-key-D0BF1D737C9A1C22.gpg.enc -out .travis/release-signing-key-D0BF1D737C9A1C22.gpg -d

mkdir -p "$HOME/.gpg"
gpg --batch --import .travis/release-signing-key-D0BF1D737C9A1C22.gpg
rm .travis/release-signing-key-D0BF1D737C9A1C22.gpg
