#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

GRADLE_VERSION="8.10.2"
GRADLE_SHA256="31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26"
DIST_ROOT="$PWD/.gradle-dist"
DIST_DIR="$DIST_ROOT/gradle-$GRADLE_VERSION"
DIST_ZIP="$DIST_ROOT/gradle-$GRADLE_VERSION-bin.zip"
MOD_VERSION="$(sed -n 's/^mod_version=//p' gradle.properties | head -n 1)"
MINECRAFT_VERSION="$(sed -n 's/^minecraft_version=//p' gradle.properties | head -n 1)"

if [ -z "$MOD_VERSION" ] || [ -z "$MINECRAFT_VERSION" ]; then
    echo "ERROR: gradle.properties does not define mod_version and minecraft_version." >&2
    exit 1
fi

EXPECTED_JAR="$PWD/build/libs/pickrelay-$MINECRAFT_VERSION-$MOD_VERSION.jar"

if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: Java is not installed or is not in PATH. Java 21 is required." >&2
    exit 1
fi

verify_gradle_zip() {
    echo "$GRADLE_SHA256  $DIST_ZIP" | sha256sum -c - >/dev/null 2>&1
}

if [ ! -x "$DIST_DIR/bin/gradle" ]; then
    mkdir -p "$DIST_ROOT"

    if [ ! -f "$DIST_ZIP" ] || ! verify_gradle_zip; then
        rm -f "$DIST_ZIP"
        echo "Downloading Gradle $GRADLE_VERSION..."
        if ! curl -L --fail --retry 3 --retry-all-errors \
            "https://downloads.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" \
            -o "$DIST_ZIP"; then
            echo "Direct Gradle host failed; trying services.gradle.org..."
            curl -L --fail --retry 3 --retry-all-errors \
                "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" \
                -o "$DIST_ZIP"
        fi
    fi

    if ! verify_gradle_zip; then
        echo "ERROR: Gradle archive checksum does not match the official $GRADLE_VERSION distribution." >&2
        rm -f "$DIST_ZIP"
        exit 1
    fi

    unzip -q -o "$DIST_ZIP" -d "$DIST_ROOT"
fi

echo "Building Pick Relay release $MOD_VERSION for Minecraft $MINECRAFT_VERSION..."
"$DIST_DIR/bin/gradle" --no-daemon clean build --stacktrace

if [ ! -f "$EXPECTED_JAR" ]; then
    echo "ERROR: Gradle finished, but the expected JAR was not found:" >&2
    echo "  $EXPECTED_JAR" >&2
    exit 1
fi

echo "DONE: $EXPECTED_JAR"
