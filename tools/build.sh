#!/usr/bin/env bash
# Use a supported JDK without changing the machine's default Java installation.
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -z "${JAVA_HOME:-}" && "$(uname -s)" == Darwin ]]; then
  for candidate in /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home /usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home; do
    if [[ -x "$candidate/bin/java" ]]; then export JAVA_HOME="$candidate"; break; fi
  done
  if [[ -z "${JAVA_HOME:-}" ]]; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || /usr/libexec/java_home -v 21 2>/dev/null || true)"
  fi
fi
if [[ -z "${ANDROID_HOME:-}" && "$(uname -s)" == Darwin && -d "$HOME/Library/Android/sdk" ]]; then
  export ANDROID_HOME="$HOME/Library/Android/sdk"
fi
if [[ $# -eq 0 ]]; then set -- assembleFdroidDebug; fi
exec ./gradlew "$@"
