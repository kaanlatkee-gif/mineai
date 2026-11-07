# Development Progress — Artificial Society

Honest status per spec rule: **implemented / compiled / unit-tested /
integration-tested / in-server-tested** are distinct tiers and are never
conflated. "—" means not yet done (never silently skipped).

Last updated: 2026-11-07.

## 1. Verification tiers used in this document

| Tier | Meaning | How it runs |
|------|---------|-------------|
| **Implemented** | Code written and reviewed against verified 1.21.1 yarn API names | — |
| **Compiled** | Actually compiled by a Java compiler in this workspace | WASM javac (simulation) / Gradle (full) |
| **Unit tested** | Assertions executed and passing | `scripts/offline-verify.sh` → AllTests |
| **Integration tested** | Runs with Minecraft/Fabric on the classpath (Loom) | `gradle build` + runClient/runServer |
| **In-server tested** | Observed in a live Minecraft 1.21.1 server | manual smoke test |

## 2. Status matrix

### Simulation engine (`simulation/`) — the separation core

| Component | Implemented | Compiled | Unit tested |
|-----------|:-----------:|:--------:|:-----------:|
| `core/Vec3`, `core/WorldSnapshot`, `core/Perceived*` | ✅ | ✅ | ✅ |
| `needs/NeedType`, `needs/Needs` (decay, clamp, effects) | ✅ | ✅ | ✅ `NeedsTest` |
| `personality/` (traits, generator, bounds, seed stability) | ✅ | ✅ | ✅ `PersonalityTest` |
| `memory/` (salience, recall, capacity pruning, threat recall) | ✅ | ✅ | ✅ `MemoryTest` |
| `social/` (directional attributes, growth, isolation decay) | ✅ | ✅ | ✅ `RelationshipTest` |
| `perception/` records | ✅ | ✅ | ✅ (via other tests) |
| `action/ActionType`, `action/NpcAction` | ✅ | ✅ | ✅ |
| `goal/` (types, manager, persistence hooks) | ✅ | ✅ | ✅ `PersistenceRoundTripTest` |
| `decision/` (utility model, jitter, variation bounds) | ✅ | ✅ | ✅ `DecisionVariationTest` |
| `NpcBrain` (create/restore/decide/feedback/explain) | ✅ | ✅ | ✅ `BrainDecisionTest` |
| `NpcState` persistence round-trip | ✅ | ✅ | ✅ `PersistenceRoundTripTest` |
| `NameGenerator` (name pool + uniqueness) | ✅ | ✅ | ✅ `PersonalityTest` |
| **Separation demo** (decide with `Class.forName("net.minecraft…")` absent) | ✅ | ✅ | ✅ `BrainDecisionTest` |

**Suite result: 7/7 passing** (offline, no Minecraft on the classpath).

### Minecraft integration (`minecraft/`)

| Component | Implemented | Compiled | Unit tested | Integration tested |
|-----------|:-----------:|:--------:|:-----------:|:------------------:|
| `ArtificialSocietyMod` entrypoint | ✅ | ⬜ | — | ⬜ |
| `entity/CitizenRegistry` (type, egg, attributes, spawn rules) | ✅ | ⬜ | — | ⬜ |
| `entity/CitizenEntity` (tick bridge, NBT hooks) | ✅ | ⬜ | — | ⬜ |
| `controller/CitizenController` (snapshot→decide→execute) | ✅ | ⬜ | — | ⬜ |
| `perception/MinecraftWorldAdapter` (World→WorldSnapshot) | ✅ | ⬜ | — | ⬜ |
| `persistence/CitizenNbtCodec` (NpcState⇄NBT, validation) | ✅ | ⬜ | — | ⬜ |
| `client/` renderer + entrypoint | ✅ | ⬜ | — | ⬜ |
| Assets (texture, spawn-egg model, lang) | ✅ | — | — | ⬜ |

⬜ = not yet verified. The Minecraft module **cannot be compiled in this
sandbox**: Gradle distribution, `repo1.maven.org`, `maven.fabricmc.net` and
Mojang libraries are unreachable from the network allowlist (documented in
the session; Gradle wrapper jar was recovered but `compileJava` still needs
Fabric/Mojang artifacts). All Minecraft API names used were verified against
GitHub code search over real 1.21.1 Fabric mods (counts in the table below),
but verification-by-search ≠ compilation.

| API used | Evidence |
|----------|----------|
| `EntityType.Builder.create(..., SpawnGroup.CREATURE)` | minefortress Colonist + others |
| `LivingEntity.createLivingAttributes()` + `GENERIC_*` | minefortress + widespread |
| `FabricDefaultAttributeRegistry.register` | fabric docs + 97 hits |
| `BiomeModifications.addSpawn` + `BiomeSelectors.foundInOverworld` | 1218 hits |
| `writeCustomDataToNbt` / `readCustomDataFromNbt` | minefortress + 750 hits |
| `EntityRendererRegistry.register` | 336 hits |
| `BipedEntityModel` + `EntityModelLayers.PLAYER` | common renderer pattern |
| `Item.getFoodComponent()` | 45 hits (1.21.x renamed from `isFood`) |
| `navigation.startMovingTo(x,y,z,speed)` | standard 1.21.1 pattern |
| `world.getTimeOfDay()` | yarn name (vs mojmap `getTimeOfDay`) |
| `swingHand(Hand.MAIN_HAND)` | 4768 hits |

### Build & docs

| Item | Status |
|------|--------|
| Gradle multi-module build (simulation standalone, loom for minecraft) | written, **not run here** (network) |
| `scripts/javac-wasm.mjs` offline compiler driver | ✅ working |
| `scripts/offline-verify.sh` | ✅ working (runs the 7/7 suite + import-boundary grep) |
| ARCHITECTURE.md / SYSTEM_DESIGN.md / DEVELOPMENT_PROGRESS.md | ✅ this commit |
| Fabric mod metadata (`fabric.mod.json`), lang, textures | ✅ written |

## 3. How to verify (reproducible)

```bash
# Tier: compiled + unit tested (simulation only, offline)
scripts/offline-verify.sh
# → "Simulation tests: 7 passed, 0 failed, 7 total"

# Tier: full build incl. Minecraft module (needs network — NOT run in this sandbox)
./gradlew build
./gradlew runClient   # or runServer for the in-server tier
```

## 4. Known problems / risks

1. **Minecraft module unverified by a compiler** — highest-risk gap. Mitigation: every yarn name was search-verified; first `./gradlew build` may still surface small compile fixes (e.g. exact `BipedEntityModel` constructor arity, `NbtDouble.of` availability).
2. **Gradle could not run in this sandbox** — wrapper jar present, but distribution + repos unreachable; `compileJava` on the bundled runtime lacks `ToolProvider.getSystemJavaCompiler()` by design (runtime-only JRE). Offline tier uses WASM javac instead.
3. **Crafting/economy/professions/schedules/dialogue** are not implemented (Phases 4–6) — intentionally out of this milestone's scope, not skipped failures.
4. **Spawn placement** is biome-wide at low weight; village-only spawning and settlement placement (Phase 5/6) pending.
5. **Citizen↔citizen pathing** relies on vanilla `PathAwareEntity` navigation — no crowd avoidance yet.

## 5. Next milestone (Phase 3 → 4)

- Compile the Minecraft module on a networked machine (`./gradlew build`), fix any residual API mismatches, run `runServer` and observe a citizen eating/fleeing (integration + in-server tiers).
- `/npc` debug command (show needs/personality/relationships, export state).
- Phase 4 systems: crafting recipes, economy/market, profession/schedule data structures feeding `WORK`/`TRADE`.
