#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
# Optional convenience defaults for the original development laptop.
# Other machines can use ./gradlew directly or set these variables themselves.
toolchain_dir="${SANGYAN_TOOLCHAIN_DIR:-$HOME/Desktop/android-toolchain}"
if [[ -z "${JAVA_HOME:-}" && -d "$toolchain_dir/jdk-17.0.20.1+1" ]]; then
 export JAVA_HOME="$toolchain_dir/jdk-17.0.20.1+1"
fi
if [[ -z "${ANDROID_HOME:-}" && -d "$toolchain_dir/sdk" ]]; then
 export ANDROID_HOME="$toolchain_dir/sdk"
fi
if [[ -z "${GRADLE_USER_HOME:-}" && -d "$toolchain_dir/gradle-home" ]]; then
 export GRADLE_USER_HOME="$toolchain_dir/gradle-home"
fi
exec ./gradlew --max-workers=2 "$@"
