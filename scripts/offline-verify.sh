#!/usr/bin/env bash
# Offline verification of the simulation engine — no Gradle, no network.
#
# Compiles every simulation source (main + tests) with the WASM javac
# toolchain and runs the unit suite. This is the check that runs in
# sandboxes without Maven Central / Gradle service access; the full
# `gradle build` (which additionally compiles the Minecraft module and
# runs Loom) is the canonical CI check — see DEVELOPMENT_PROGRESS.md
# for the honest status of each verification tier.
#
# Requirements:
#   JAVAC_WASM_DIR   teavm-javac toolchain dir (contains compiler.wasm,
#                    classlib-*.zip, runtime)
#   JDK21_HOME       a real Java 21 runtime (default: /home/user/tools/jdk21)
#   node             JavaScript runtime for the compiler driver
set -euo pipefail
cd "$(dirname "$0")/.."

JAVAC_WASM_DIR=${JAVAC_WASM_DIR:-/home/user/tools/javac-wasm}
JDK21_HOME=${JDK21_HOME:-/home/user/tools/jdk21}
export JAVAC_WASM_DIR

if [ ! -f "$JAVAC_WASM_DIR/compiler.wasm" ]; then
  echo "offline-verify: compiler.wasm not found in $JAVAC_WASM_DIR" >&2
  exit 2
fi
if [ ! -x "$JDK21_HOME/bin/java" ]; then
  echo "offline-verify: java runtime not found at $JDK21_HOME/bin/java" >&2
  exit 2
fi

echo "== checking simulation package for Minecraft imports =="
# Only real import statements count; BrainDecisionTest legitimately mentions
# "net.minecraft.entity.Entity" inside Class.forName as a *negative* check.
if grep -rn "^import .*\bnet\.\(minecraft\|fabricmc\)" simulation/src >/dev/null 2>&1; then
  echo "offline-verify: FAIL — Minecraft import found in simulation sources:" >&2
  grep -rn "^import .*\bnet\.\(minecraft\|fabricmc\)" simulation/src >&2 || true
  exit 1
fi
echo "clean (no Minecraft/Fabric imports)"

echo "== compiling simulation main sources =="
rm -rf build/offline
mkdir -p build/offline/main build/offline/test
node scripts/javac-wasm.mjs -d build/offline/main \
  $(find simulation/src/main/java -name '*.java' | sort)

echo "== compiling simulation test sources =="
node scripts/javac-wasm.mjs -d build/offline/test -cp build/offline/main \
  $(find simulation/src/test/java -name '*.java' | sort)

echo "== running simulation unit suite =="
"$JDK21_HOME/bin/java" -cp build/offline/main:build/offline/test \
  artificial.society.simulation.tests.AllTests

echo "offline-verify: OK"
