#!/usr/bin/env sh
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"
MAVEN_VERSION="3.9.9"
MAVEN_DIR="$BASE_DIR/.mvn/wrapper/dists/apache-maven-$MAVEN_VERSION"
MAVEN_ZIP="$BASE_DIR/.mvn/wrapper/dists/apache-maven-$MAVEN_VERSION-bin.zip"

if ! command -v java >/dev/null 2>&1; then
  echo "Error: Java 21 is required but java was not found in PATH." >&2
  echo "Install a JDK 21 or set JAVA_HOME before running ./mvnw." >&2
  exit 1
fi

mkdir -p "$BASE_DIR/.mvn/wrapper/dists"

if [ ! -x "$MAVEN_DIR/bin/mvn" ]; then
  DISTRIBUTION_URL=$(sed -n 's/^distributionUrl=//p' "$WRAPPER_PROPERTIES")
  if [ -z "$DISTRIBUTION_URL" ]; then
    echo "Error: distributionUrl not found in $WRAPPER_PROPERTIES" >&2
    exit 1
  fi

  if [ ! -f "$MAVEN_ZIP" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$DISTRIBUTION_URL" -o "$MAVEN_ZIP"
    else
      echo "Error: curl is required to download Maven $MAVEN_VERSION." >&2
      exit 1
    fi
  fi

  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$MAVEN_ZIP" -d "$BASE_DIR/.mvn/wrapper/dists"
  else
    echo "Error: unzip is required to extract Maven $MAVEN_VERSION." >&2
    exit 1
  fi
fi

exec "$MAVEN_DIR/bin/mvn" "$@"
