#!/usr/bin/env bash

# Exit this script immediately if a command/function exits with a non-zero status.
set -e

SCRIPT_INCLUDES="log.bash utils.bash setup-secrets.bash openjdk.bash maven.bash"
# shellcheck source=inc/fetch_ci_scripts.bash
source "$(dirname "$0")/inc/fetch_ci_scripts.bash" && fetch_ci_scripts

function build() {
    pmd_ci_log_group_start "Install OpenJDK"
        pmd_ci_openjdk_install_adoptopenjdk 11
        pmd_ci_openjdk_setdefault 11
    pmd_ci_log_group_end

    echo
    pmd_ci_maven_display_info_banner
    pmd_ci_utils_determine_build_env pmd/pmd-designer
    echo

    if pmd_ci_utils_is_fork_or_pull_request; then
        pmd_ci_log_group_start "Build with mvnw"
            ./mvnw clean verify --show-version --errors --batch-mode --no-transfer-progress
        pmd_ci_log_group_end
        exit 0
    fi

    # only builds on pmd/pmd-designer continue here
    pmd_ci_log_group_start "Setup environment"
        pmd_ci_setup_secrets_private_env
        pmd_ci_setup_secrets_gpg_key
        pmd_ci_maven_setup_settings
    pmd_ci_log_group_end

    # snapshot or release - it only depends on the version (SNAPSHOT or no SNAPSHOT)
    # the build command is the same
    pmd_ci_log_group_start "Build with mvnw"
        pmd_ci_maven_verify_version
        ./mvnw clean deploy -Psign --show-version --errors --batch-mode --no-transfer-progress
    pmd_ci_log_group_end
}

build

exit 0
