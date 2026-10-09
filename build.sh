#!/usr/bin/env bash
# Wrapper around ./gradlew that makes sure a JDK 21 is used.
#
# Gradle 8.13 (see gradle/wrapper/gradle-wrapper.properties) cannot run on
# very new JDKs (e.g. JDK 26 fails with "Type T not present"), and the
# project itself targets sourceCompatibility/targetCompatibility 21
# (see build.gradle). This script locates a JDK 21 and exports JAVA_HOME
# for the ./gradlew invocation, so contributors don't have to do it by hand.
#
# Usage: ./build.sh [gradle args...]
#   ./build.sh build
#   ./build.sh build -x test
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

find_jdk21() {
    # 1) explicit override
    if [ -n "${JDK21_HOME:-}" ] && [ -x "${JDK21_HOME}/bin/java" ]; then
        echo "${JDK21_HOME}"
        return
    fi
    # 2) portable JDK downloaded by earlier setup (see repo history / chat log)
    for d in "${HOME}"/.jdks/jdk-21*/Contents/Home "${HOME}"/.jdks/jdk-21*; do
        if [ -x "${d}/bin/java" ]; then
            echo "${d}"
            return
        fi
    done
    # 3) Homebrew (macOS)
    if [ -x /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin/java ]; then
        echo /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
        return
    fi
    if [ -x /usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin/java ]; then
        echo /usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
        return
    fi
    # 4) macOS java_home helper
    if command -v /usr/libexec/java_home >/dev/null 2>&1; then
        if home=$(/usr/libexec/java_home -v 21 2>/dev/null); then
            echo "${home}"
            return
        fi
    fi
    # 5) Linux default location (apt openjdk-21-jdk, matches Dockerfile)
    if [ -x /usr/lib/jvm/java-21-openjdk-amd64/bin/java ]; then
        echo /usr/lib/jvm/java-21-openjdk-amd64
        return
    fi
    return 1
}

if JDK21_HOME_FOUND=$(find_jdk21); then
    export JAVA_HOME="${JDK21_HOME_FOUND}"
else
    echo "build.sh: could not find a JDK 21 install." >&2
    echo "  Set JDK21_HOME to point at one, e.g.:" >&2
    echo "    JDK21_HOME=/path/to/jdk-21 ./build.sh build" >&2
    echo "  Or download one, e.g. via https://adoptium.net/temurin/releases/?version=21" >&2
    exit 1
fi

echo "build.sh: using JAVA_HOME=${JAVA_HOME}" >&2
exec ./gradlew "$@"
