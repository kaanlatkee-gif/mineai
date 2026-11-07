# System Design — Artificial Society

## 1. Core entities

### NpcBrain (root of the simulation)
Owns one NPC's entire mental state: `NpcIdentity`, `Personality`, `Needs`,
`MemorySystem`, `RelationshipManager`, `GoalManager`, the current action and
a tick counter. Public surface used by the Minecraft layer:

```java
NpcBrain.create(NpcIdentity, Vec3 home, int personalitySeed)  // fresh spawn
NpcBrain.restore(NpcState)                                     // from NBT
WorldSnapshot -> advanceTicks(WorldSnapshot, int ticks)        // need/status update
WorldSnapshot -> decide(WorldSnapshot) -> NpcAction            // decision cycle
onAte(String itemId, int nutrition)                            // eating feedback
onMove(Vec3)                                                   // movement feedback
noteInteraction(String subjectId, String name, SocialInteractionType,
                double magnitude, boolean initiatedBySelf)     // social feedback
NpcState toState()                                             // persistence export
String explainLastDecision()                                    // debug report
```

### NpcState — the persistence interchange
A plain record with no behavior and no Minecraft types: identity, spawn tick,
occupation, home position, `double[] needs` (NeedType ordinal order),
`double[] personality` (TraitType ordinal order), current action type,
memories, relationships, goals. This is the unit that survives restarts and
is the only shape `CitizenNbtCodec` serialises.

### WorldSnapshot — the perception interchange
Immutable view built by `MinecraftWorldAdapter` every 10 ticks:
self/home position, time of day, perceived agents (fellow citizens, players,
monsters with hostility estimate), food items (id, position, distance,
nutrition, count), threats (kind, position, distance, severity), plus cached
flags (`hasThreats`, `nearestThreatDistance`, `hasFood`, `nearestFood`,
`friendNearby`) so decision scoring never rescans lists.

### NpcAction — the decision output
`ActionType` (IDLE, WORK, EXPLORE, EAT, SOCIALIZE, RETURN_HOME, SLEEP, REST,
TRADE, GATHER_RESOURCES, AVOID_DANGER, ASSIST_OTHER), optional `Vec3 target`,
optional `subjectId` (for social types), `score` and `explanation`. Pure data
— the controller maps it to game effects.

## 2. Decision loop (UtilityAI)

1. `advanceTicks`: needs decay toward baseline (SPEC 0.9×), clamped 0–100,
   night/day comfort effects (DAYTIME/COMFORT/SOCIAL), and status effects
   derived from the current action (eating restores HUNGER from nutrition
   carried in the action; REST/SLEEP restore ENERGY/COMFORT).
2. `decide`:
   - candidate actions are generated from state (e.g. EAT whenever visible
     food exists; AVOID_DANGER while any threat is perceived);
   - `UtilityEvaluator.score(...)` produces the raw utility for each;
   - `UtilityEvaluator.choose` applies seeded ±7.5 jitter
     (`DecisionEngine.JITTER_SEED` + tick), a small continuity bonus for the
     current action and a small initiative boost — jitter can *tie-break* but
     never overturns a clear need (verified by `DecisionVariationTest`);
   - the winner becomes `activeAction`, is persisted into `NpcState` and its
     explanation is available via `explainLastDecision()`.
3. The controller executes the action and feeds results back (`onAte`,
   `onMove`, `noteInteraction`), closing the loop without the brain ever
   seeing an Minecraft object.

## 3. Scoring model (excerpt, full table in `UtilityEvaluator`)

| Action        | Driver |
|---------------|--------|
| EAT           | HUNGER urgency (0.65 + food factor) × presence of visible food |
| SLEEP/REST    | darkness × ENERGY deficit + comfort deficit |
| SOCIALIZE     | SOCIAL deficit × sociability (×0.8) + kindness/warmth |
| WORK          | baseline drive + ambition×30 + patience×8 + energy |
| EXPLORE       | curiosity×12 + high SOCIAL × (1 − sociability) |
| GATHER        | hunger + low storage + diligence |
| AVOID_DANGER  | severity × (1.6 − bravery) + fear + remembered-threat boost |
| RETURN_HOME   | safety + homesickness × (1 − curiosity) at night |
| TRADE         | need for food/gear × honesty/trustworthiness |
| ASSIST        | proximity × (kindness + agreeableness) × target distress |

## 4. Memory & relationships

- `MemorySystem` holds up to `MEMORY_CAPACITY` (50) salient memories; recall
  scores importance × recency (exponential, `MEM_HALF_LIFE` 500 ticks) ×
  emotional impact; capacity pressure prunes the oldest/weakest first
  (verified by `MemoryTest`).
- Threat recall: a remembered hostile event re-arms the AVOID_DANGER score
  for `THREAT_MEMORY_TICKS` (500).
- `Relationship` is directional with six attributes
  (friendship/trust/respect/fear/rivalry/familiarity) and an isolation-decay
  rule applied when a relationship has not been touched for
  `ISOLATION_DECAY_INTERVAL` (500 ticks).

## 5. Persistence format (NBT)

Under the entity root, key `ArtificialSociety`:
`id`, `name`, `spawnTick`, `occupation`, `home{x,y,z}`, `needs[]`,
`personality[]`, `currentAction`, `memories[]` (id, category, description,
participants, location, day, tick, impact, importance), `relationships[]`
(targetId + six attributes + timestamps), `goals[]` (type, priority,
createdTick, target, subjectId).

Validation on read: missing id/name → treat as fresh spawn; corrupt memory /
goal entries are skipped individually; arrays are length-checked against the
current enums and clamped by `Needs`/`Personality` constructors (spec §7.5:
all persistent data validated and recoverable).

## 6. Error handling (spec §7.4)

- **No MC import in simulation** → enforced by build + test.
- **Failed update** → a per-entity decision failure leaves the NPC on its
  last action; it does not crash the tick loop (controller guards every
  phase; `NpcBrain.decide` is total over its inputs).
- **Invalid persistence** → codec returns `null` state → fresh deterministic
  identity derived from the entity UUID (stable name/personality).
- **External interference** (commands, other mods) → NBT is re-read only on
  entity load; the brain owns its state while alive and re-exports on save.
- **Redundant updates** → staggered schedules + perception caching (above).
- **World conflicts** → only the server thread touches the brain; client
  classes are isolated in `artificial.society.minecraft.client`.

## 7. Configuration knobs (spec §7.6)

`DecisionEngine.JITTER_SEED` (decision variation seed), `Needs` decay rates,
`MemorySystem` capacity/half-life, `Relationship` decay interval,
`MinecraftWorldAdapter.PERCEPTION_RADIUS` (16 blocks), controller intervals
(NEED 20 / PERCEPTION 10 / DECISION 40 / DANGER 10 / SOCIAL_COOLDOWN 100).

## 8. Out of scope for this milestone (tracked, not skipped)

Crafting recipes, economy/market trading mechanics, professions & schedules
(occupation only recorded), village settlement planning, `/npc` debug
commands, day/night schedule automation, speech/dialogue generation, LLM
hooks — see DEVELOPMENT_PROGRESS.md for phase status.
