#!/usr/bin/env bash
# In-environment unit tests WITHOUT JUnit/Maven.
# Compiles the plugin + a no-dependency harness and runs it. The harness re-verifies the pure logic
# AND that every bundled example mob/horde parses + cross-references resolve.
# (Canonical tests are the JUnit suite under src/test/java, run via `mvn test`.)
#
# Uses RELATIVE paths from the plugin dir — a Windows JDK from Git Bash rejects MSYS absolute paths.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$HERE"

case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
CP="$(find ../libraries -name '*.jar' | tr '\n' "$SEP")"

rm -rf build/test-classes
mkdir -p build/test-classes

echo ">> Compiling main + verify harness"
{ find src/main/java -name '*.java'; find tools/verify -name '*.java'; } > build/test-sources.txt
javac --release 21 -encoding UTF-8 -cp "$CP" -d build/test-classes @build/test-sources.txt

echo ">> Running harness"
java -cp "build/test-classes$SEP$CP" me.zygotecode.amazingmobs.verify.VerifyMain src/main/resources
