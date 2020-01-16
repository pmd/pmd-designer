#
# OpenJDK by AdoptOpenJDK (https://adoptopenjdk.net/)
#

# VERSION_TAG e.g. "11.0.4+11" or "13+33"
VERSION_TAG=11.0.6+10
OPENJDK_MAJOR=${VERSION_TAG/.*/}
OPENJDK_MAJOR=${OPENJDK_MAJOR/+*/}
#BASE_URL=https://github.com/AdoptOpenJDK/openjdk${OPENJDK_MAJOR}-binaries/releases/download
BASE_URL=https://pmd-code.org/openjdk


DOWNLOAD_URL=${BASE_URL}/jdk-${VERSION_TAG/+/%2B}/OpenJDK${OPENJDK_MAJOR}U-jdk_x64_linux_hotspot_${VERSION_TAG/+/_}.tar.gz
COMPONENTS_TO_STRIP=1 # e.g. openjdk-11.0.3+7/bin/java

OPENJDK_ARCHIVE=$(basename $DOWNLOAD_URL)

LOCAL_DIR=${HOME}/.cache/openjdk
TARGET_DIR=${HOME}/openjdk11

mkdir -p ${LOCAL_DIR}
mkdir -p ${TARGET_DIR}
wget --directory-prefix ${LOCAL_DIR} --timestamping --continue ${DOWNLOAD_URL}
tar --extract --file ${LOCAL_DIR}/${OPENJDK_ARCHIVE} -C ${TARGET_DIR} --strip-components=${COMPONENTS_TO_STRIP}

export JAVA_HOME=${TARGET_DIR}
export PATH=${JAVA_HOME}/bin:$PATH

java -version
