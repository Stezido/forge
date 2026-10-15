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
case "${1:-launch}" in
  build)
    cd "$root"
    exec mvn -B -pl forge-gui-desktop -am -DskipTests package
    ;;
  test)
    cd "$root"
    export XDG_CONFIG_HOME="$runtime/test-config"
    export XDG_CACHE_HOME="$runtime/test-cache"
    exec mvn -B -pl forge-gui-desktop -am -Dtest=ChaosBrawlTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
    ;;
  launch)
    if [[ ! -f "$runtime/run/forge.profile.properties" ]]; then
      echo 'Prepare the isolated deck/profile with the website prepare.mjs helper first.' >&2
      exit 1
    fi
    jars=("$root"/forge-gui-desktop/target/*-jar-with-dependencies.jar)
    if [[ ${#jars[@]} != 1 || ! -f "${jars[0]}" ]]; then
      echo 'Build Forge first with: bash chaosbrawl/forge.sh build' >&2
      exit 1
    fi
    cd "$runtime/run"
    exec java -Xmx4g -Dforge.chaosbrawl=true -Dio.netty.tryReflectionSetAccessible=true -Dfile.encoding=UTF-8 -jar "${jars[0]}"
    ;;
  *) echo 'Usage: bash chaosbrawl/forge.sh [build|test|launch]' >&2; exit 1 ;;
esac
