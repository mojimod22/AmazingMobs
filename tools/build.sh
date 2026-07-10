#!/usr/bin/env bash
# In-environment build for AmazingMobs WITHOUT Maven/Gradle.
# Compiles against the server's bundled jars (../libraries) and packages AmazingMobs.jar.
#
# Usage:  bash tools/build.sh [--deploy]
# Canonical build is still `mvn package` (see pom.xml); this exists so the plugin can be built and
# verified inside the provided Paper server folder, which ships no build tool.
#
# Note: we cd into the plugin dir and use RELATIVE paths on purpose — a Windows JDK invoked from
# Git Bash does not accept MSYS-style absolute paths (/c/Users/...), and relative paths also avoid
# spaces in the project path.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"   # plugin/
cd "$HERE"

if [ ! -d "../libraries" ]; then echo "ERROR: ../libraries not found (run from inside the Paper server folder)"; exit 1; fi

# Classpath separator: ';' on Windows shells calling a Windows JDK, ':' elsewhere.
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
CP="$(find ../libraries -name '*.jar' | tr '\n' "$SEP")"

echo ">> Cleaning build/"
rm -rf build/classes build/AmazingMobs.jar
mkdir -p build/classes

echo ">> Compiling sources (--release 21)"
find src/main/java -name '*.java' > build/sources.txt
javac --release 21 -encoding UTF-8 -cp "$CP" -d build/classes @build/sources.txt

echo ">> Copying resources"
VERSION="$(grep -m1 '<version>' pom.xml | sed -E 's/.*<version>(.*)<\/version>.*/\1/')"
( cd src/main/resources && cp -r . "$HERE/build/classes/" )
sed -i "s/\${project.version}/$VERSION/g" build/classes/plugin.yml

echo ">> Packaging build/AmazingMobs.jar"
( cd build/classes && jar --create --file ../AmazingMobs.jar . )
echo ">> Built: build/AmazingMobs.jar"

if [ "${1:-}" = "--deploy" ]; then
  mkdir -p ../plugins
  cp build/AmazingMobs.jar ../plugins/AmazingMobs.jar
  echo ">> Deployed to ../plugins/AmazingMobs.jar"
fi
