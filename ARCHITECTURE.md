# Architecture — Artificial Society

## Layering (strict, enforced by the build + tests)

```
┌────────────────────────────────────────────────────────────────┐
│ minecraft/  (artificial.society.minecraft.*)                   │
│   entity/         CitizenEntity, CitizenRegistry (registration)│
│   controller/     CitizenController   — action translation     │
│   perception/     MinecraftWorldAdapter — world → snapshot     │
│   persistence/    CitizenNbtCodec     — NpcState ⇄ NBT         │
│   client/         renderer + client entrypoint                 │
│   ← the ONLY packages allowed to import net.minecraft / fabric │
├────────────────────────────────────────────────────────────────┤
│ simulation/  (artificial.society.simulation.*)                 │
│   core/       Vec3, WorldSnapshot, Perceived* records          │
│   needs/      NeedType, Needs                                   │
│   personality/ TraitType, Personality, PersonalityGenerator    │
│   memory/     Memory, MemoryImportance, MemorySystem           │
│   social/     Relationship, RelationshipManager, interactions  │
│   perception/ AgentKind, PerceivedAgent/Item/Threat             │
│   action/     ActionType, NpcAction (abstract actions)         │
│   goal/       Goal, GoalType, GoalManager                      │
│   decision/   ActionScore, DecisionContext, UtilityEvaluator,  │
│               DecisionEngine                                   │
│   NpcBrain, NpcState, NpcIdentity, NameGenerator               │
│   ← ZERO Minecraft/Fabric/rendering/entity/world imports       │
└────────────────────────────────────────────────────────────────┘
```

## The nine separation rules (spec §7.1) and where they live

1. **Standalone `simulation` package** — everything above lives in
   `simulation/src/main/java/artificial/society/simulation`.
2. **Zero MC imports there** — the module has no Fabric/Minecraft
   dependencies in `simulation/build.gradle`; `BrainDecisionTest` proves it at
   runtime by asserting `Class.forName("net.minecraft.entity.Entity")` throws
   `ClassNotFoundException`.
3. **Ordinary Java data structures** — brains take `WorldSnapshot`, `Needs`,
   `Memory`, `Relationship` records; positions are the `Vec3` record
   (`x/y/z` doubles), never `Vec3d`/`BlockPos`.
4. **Separate `minecraft` package** — entity registration/movement/world
   perception/block interaction/rendering are all under
   `artificial.society.minecraft.*`.
5. **Controller/adapter layer** — `CitizenController` translates abstract
   `NpcAction`s into navigation calls, item consumption, hand swings and
   relationship events; `MinecraftWorldAdapter` translates `World` into
   `WorldSnapshot`.
6. **World → simple data first** — the adapter runs before any brain call;
   `DecisionContext` (decision engine input) has no Minecraft types at all.
7. **Brain returns abstract actions** — `NpcAction` records contain only
   `ActionType`, a `Vec3 target`, a subject id and a score. Nothing in
   `simulation` can reference `Entity`.
8. **Unit-testable without Minecraft** — `scripts/offline-verify.sh` compiles
   and runs the full suite with no MC classpath (7/7 passing, see
   DEVELOPMENT_PROGRESS.md).
9. **Persistent data independent of MC entity classes** — `NpcState` is a
   pure record (id, name, spawn tick, occupation, home, needs[], personality[],
   current action, memories, relationships, goals). `CitizenNbtCodec` maps it
   to/from NBT; `NpcBrain.create/restore` round-trips it and
   `PersistenceRoundTripTest` verifies a simulated restart without Minecraft.

## Data flow (one decision cycle)

```
MinecraftWorldAdapter.build(observer, tick)      [minecraft]
        └─ WorldSnapshot (plain records)          [simulation.core]
             ├─ NpcBrain.advanceTicks(snapshot, dt)   — needs decay + status effects
             └─ NpcBrain.decide(snapshot)             — utility scoring + jitter
                  └─ NpcAction (abstract)             [simulation.action]
                       └─ CitizenController.execute  [minecraft] → navigation / eat / interact
```

## Scheduling (spec §7.3)

| Loop              | Interval                | Where                        |
|-------------------|-------------------------|------------------------------|
| Movement          | every tick              | Minecraft navigation (smoothed) |
| Perception        | every 10 ticks          | `CitizenController.tick`     |
| Needs             | every 20 ticks          | `NpcBrain.advanceTicks`      |
| Decision          | 40 ticks (10 if threat) | `NpcBrain.decide`            |
| Staggering        | `entity.id % 40` offset | `CitizenController.tick`     |

## Module/build boundary

`settings.gradle` includes `:minecraft` only when `minecraft/` is present and
`-PsimulationOnly` is not set; `minecraft/build.gradle` enables Fabric Loom
only when the Fabric API/Gradle service repositories are reachable
(`-PskipMinecraftBuild`). The simulation module therefore always builds
standalone with zero external dependencies.

## Boundary enforcement summary

- Compile time: `simulation` has no MC libraries on its classpath.
- Test time: `BrainDecisionTest` loads `net.minecraft.entity.Entity` and
  expects `ClassNotFoundException` — the separation demo required by spec.
- Review time: `grep -r "net\.minecraft\|net\.fabricmc" simulation/src` must
  return nothing (part of `scripts/offline-verify.sh` checks in CI).
