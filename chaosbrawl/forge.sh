#!/usr/bin/env bash
set -euo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
tools="${CHAOSBRAWL_FORGE_TOOLS:-$HOME/.local/share/chaosbrawl/forge-tools}"
if [[ -z "${JAVA_HOME:-}" && -x "$tools/jdk/bin/java" ]]; then
  export JAVA_HOME="$tools/jdk"
fi
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
if [[ -x "$tools/maven/bin/mvn" ]]; then export PATH="$tools/maven/bin:$PATH"; fi
runtime="$root/.chaosbrawl-local"
mkdir -p "$runtime"
export SENTRY_ENABLED=false
case "${1:-launch}" in
  build)
    cd "$root"
    exec mvn -B -pl forge-gui-desktop -am -DskipTests package
    ;;
  test|test-limited)
    cd "$root"
    profile="$root/forge-gui/forge.profile.properties"
    if [[ -f "$profile" ]] && ! grep -q '^# Chaos Brawl isolated test profile$' "$profile"; then
      echo 'A source-development profile already exists. Run tests in a separate checkout.' >&2
      exit 1
    fi
    printf '# Chaos Brawl isolated test profile\nuserDir=%s/test-user\ncacheDir=%s/test-cache\n' "$runtime" "$runtime" > "$profile"
    suite=ChaosBrawlTest
    if [[ "$1" == test-limited ]]; then suite=ChaosBrawlLimitedTest; fi
    exec mvn -B -pl forge-gui-desktop -am -Dtest="$suite" -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
    ;;
  launch|smoke)
    if [[ ! -f "$runtime/run/forge.profile.properties" ]]; then
      echo 'Prepare the isolated deck/profile with python3 chaosbrawl/prepare.py first.' >&2
      exit 1
    fi
    jars=("$root"/forge-gui-desktop/target/*-jar-with-dependencies.jar)
    if [[ ${#jars[@]} != 1 || ! -f "${jars[0]}" ]]; then
      echo 'Build Forge first with: bash chaosbrawl/forge.sh build' >&2
      exit 1
    fi
    cd "$runtime/run"
    if [[ "${1:-launch}" == smoke ]]; then
      exec java -Xmx4g -Dforge.chaosbrawl=true -Dsentry.enabled=false -jar "${jars[0]}" sim -d "$(cat "$runtime/active-deck.txt")" "$(cat "$runtime/active-deck.txt")" -n 1 -s 20261016
    fi
    exec java -Dsentry.enabled=false -Xmx4g -Dforge.chaosbrawl=true -Dio.netty.tryReflectionSetAccessible=true -Dfile.encoding=UTF-8 -jar "${jars[0]}"
    ;;
  *) echo 'Usage: bash chaosbrawl/forge.sh [build|test|test-limited|smoke|launch]' >&2; exit 1 ;;
esac
