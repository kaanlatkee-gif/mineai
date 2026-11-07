# Artificial Society — Minecraft Fabric 1.21.1

A Fabric 1.21.1 mod that simulates a persistent NPC "society": citizens with
needs, personality, memory, relationships and goals that decide their own
actions — all driven by a **pure-Java simulation engine** that is strictly
separated from Minecraft.

## Repository layout

```
simulation/     Pure Java 21 simulation engine — zero Minecraft/Fabric imports.
                Unit-testable without launching Minecraft (scripts/offline-verify.sh).
minecraft/      Fabric integration: entity, perception adapter, controller,
                NBT persistence, client renderer, assets.
scripts/        Offline verification tooling (WASM javac driver + verify script).
docs/           ARCHITECTURE.md, SYSTEM_DESIGN.md, DEVELOPMENT_PROGRESS.md
                (top-level copies at repository root).
```

## Build

Full build (requires network for Gradle/Mojang/Fabric repositories):

```bash
./gradlew build                    # compiles simulation + minecraft, runs tests
./gradlew :simulation:build        # simulation module only
./gradlew build -PsimulationOnly   # same, skips the minecraft module entirely
```

Offline verification of the simulation engine (no Gradle, no network):

```bash
JDK21_HOME=/path/to/jdk21 JAVAC_WASM_DIR=/path/to/javac-wasm \
  scripts/offline-verify.sh
```

## Status

Honest, tiered status (implemented / compiled / unit-tested /
integration-tested / in-server-tested) lives in
[DEVELOPMENT_PROGRESS.md](DEVELOPMENT_PROGRESS.md). In short: the simulation
engine is compiled and unit-tested offline; the Minecraft module is written
against verified 1.21.1 yarn APIs but has not been compiled or run in a real
server from this sandbox (Gradle/Mojang/Fabric repositories are unreachable
here).

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for the layering rules and
[SYSTEM_DESIGN.md](SYSTEM_DESIGN.md) for data flow, decision loop and
persistence format.
