#
# AdoptOpenJDK Builds from:
# https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/tag/jdk-11.0.4%2B11
#

DOWNLOAD_URL=https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.4%2B11/OpenJDK11U-jdk_x64_linux_hotspot_11.0.4_11.tar.gz
OPENJDK_ARCHIVE=$(basename $DOWNLOAD_URL)

LOCAL_DIR=${HOME}/.cache/openjdk
TARGET_DIR=${HOME}/openjdk11

mkdir -p ${LOCAL_DIR}
mkdir -p ${TARGET_DIR}
wget --quiet --directory-prefix ${LOCAL_DIR} --timestamping --continue ${DOWNLOAD_URL}
tar --extract --file ${LOCAL_DIR}/${OPENJDK_ARCHIVE} -C ${TARGET_DIR} --strip-components 1

export JAVA_HOME=${TARGET_DIR}
export PATH=${JAVA_HOME}/bin:$PATH

java -version
