# AmazingMobs — Architecture & Design Notes

> Target: **Paper 1.21.11** (`paper-api:1.21.11-R0.1-SNAPSHOT`), Java 21 bytecode.
> Package root: `eu.breakapp.amazingmobs`.

This document records the *why* behind the structure. It is written for maintainers and
contributors. End-user docs live in `README.md`, `docs/MOBS.md`, `docs/HORDES.md`,
`docs/COMMANDS.md`, `docs/PERMISSIONS.md`.

---

## 1. Product goals (non-negotiable)

AmazingMobs is a **large-scale horde event engine** for medium/large Paper servers. It must:

- Run massive, readable, *balanceable* horde events over wide areas (~500 block radius, configurable).
- Provide an advanced **custom mob** system (identity, stats, gear, AI, skills, drops, phases).
- Offer **two authoring paths**: in-game commands *and* official config files.
- Ship **20 example mobs** + **30 example hordes**, varied and production-grade.
- Be **performance-conscious**, **fail-safe**, **validated**, **documented**, **extensible**.

It is a product, not a demo. No empty stubs on the critical path.

---

## 2. Configuration format decision — YAML (official, single source of truth)

We evaluated **YAML** vs **JSON** for the official mob/horde config format.

| Criterion | YAML | JSON |
|---|---|---|
| Comments (admins annotate balance) | ✅ native `#` | ❌ none |
| Manual editing ergonomics | ✅ terse, indentation | ⚠️ brace/quote noise |
| Paper/Bukkit ecosystem fit | ✅ native (`YamlConfiguration`, plugin.yml, every Bukkit plugin) | ⚠️ foreign, needs custom loader |
| Reliable parsing | ✅ SnakeYAML ships with the server | ✅ Gson ships too |
| Validation | ✅ same as JSON (we validate the parsed map) | ✅ |
| Readability for big nested wave lists | ✅ | ⚠️ deep nesting gets noisy |

**Decision: YAML is the one official format** (`.yml`). It is the idiomatic Paper choice,
supports comments (critical for a balance-heavy content pack), and uses the parser the
server already bundles.

**JSON is deliberately NOT introduced.** A second format here would be decorative: it would
duplicate the schema, double the validation surface, and split example content — all cost,
no real benefit for hand-authored content. We considered JSON for *programmatic interchange*
(export/import between servers) but `/am export` already round-trips through the **same YAML
pipeline** that loads files, which is strictly safer (export → drop in `/mobs` → reload). So
the export format is YAML too. This keeps exactly one schema, one validator, one loader.

> If a future need for machine interchange appears (e.g. a web editor), JSON export can be
> added as a *projection* of the same in-memory model — never as a second authoring format.

### Decoupling for testability
Parsers never touch `YamlConfiguration` directly. `ConfigSource` converts a `.yml` file into a
plain `Map<String,Object>` (via Bukkit's bundled SnakeYAML at runtime), wrapped by
`ConfigSection`. All parsing/validation logic operates on `ConfigSection`, which can be built
from a hand-made `Map` in tests — **no running server required** to unit-test the pipeline.

---

## 3. Module map

```
eu.breakapp.amazingmobs
├─ AmazingMobs            plugin entrypoint (bootstrap, wiring, lifecycle)
├─ AmazingMobsApi         stable facade for other plugins
├─ config/               PluginConfig, Messages, ConfigSection, ConfigSource, validation model
├─ util/                 Text, Numbers, Rng, Schedulers, Keys, Cooldown
├─ mob/                  MobDefinition, StatBlock, Equipment, ItemSpec, AiProfile, Phase,
│  │                     DropTable, Tier; MobRegistry; MobParser; MobSpawner; MobManager; ActiveMob
│  └─ ...
├─ skill/                Skill, SkillType, SkillContext, SkillDefinition, SkillInstance,
│  │                     SkillRegistry, AbstractSkill
│  └─ impl/              concrete data-driven skills (projectiles, control, movement, summon, ...)
├─ area/                 AreaSpec, Shape (pure geometry), SpawnFinder (world binding)
├─ horde/               HordeDefinition, Wave, WaveEntry, HordeRegistry, HordeParser
│  └─ runtime/           HordeManager, HordeInstance, HordeScheduler
├─ scaling/              Scaling (pure curves: player-count / difficulty / time)
├─ command/             AmazingMobsCommand (root + tab complete), SubCommand, sub/*
├─ listener/            CombatListener, MobLifecycleListener, WorldBindListener
├─ api/event/           HordeStartEvent, HordeStopEvent, WaveCompleteEvent, CustomMobSpawnEvent,
│                        CustomMobDeathEvent, SkillTriggerEvent, BossPhaseChangeEvent
└─ persistence/         (PDC keys live in util/Keys; rebind logic in WorldBindListener+MobManager)
```

### 1.1.0 additions (the expansion)
- `trait/` — modular behaviour affixes mirroring the skill engine (`Trait`, `TraitRegistry`,
  `TraitContext`, `TraitInstance`, `AbstractTrait`, `impl/*`). Hooks the mob lifecycle; PDC-based
  ally detection keeps it decoupled from `mob.runtime`. Ticked via a reused per-mob context (no
  hot-path allocation).
- `mob/` gains `Variant` (spawn-time mutation overlay — a pure `MobDefinition → MobDefinition`
  transform), `SpawnConditions` (biome/time/weather/Y/world/player gating), `MountSpec`/`RiderSpec`/
  `RiderDeathBehavior` (mount + stack chains). `MobDefinition` gained `traits/variants/mount/riders`
  + `toBuilder()`; all optional → fully backward compatible.
- `MobManager` rolls a variant at spawn, assembles mount/stack passenger chains, and applies
  cascading death chains (depth + self-ride guarded; link maps cleaned on stop/reload/despawn).
- `skill/impl` gains `StrikeSkill` (controller melee — lets goal-less mobs fight), `OrbitSkill`,
  `LaunchMobSkill` (mob-as-projectile with a bounded per-throw flight task).
- `horde/` gains `DirectorSettings`; `HordeInstance` tracks a spawn multiplier adapted per wave and
  supports pause/resume.

### Layering rules
- **Pure layer** (no Bukkit): `config.ConfigSection`, `config.validation.*`, `util.Numbers`,
  `util.Rng`, `area.Shape`, `scaling.Scaling`. Unit-tested directly.
- **Model layer** (Bukkit enums OK, no live server state): `mob.*` definitions, `skill.*Definition`,
  `horde.*Definition`, parsers. Built once at load, immutable thereafter.
- **Runtime layer** (live server): spawners, managers, listeners, skill execution.

---

## 4. Mob definition engine

`MobDefinition` is immutable and fully describes a mob:

- **Identity**: `id`, `displayName` (MiniMessage), `lore`, base `EntityType`, `Tier`, `category`, `tags`.
- **Stats** (`StatBlock`): health, attackDamage, movementSpeed, knockbackResistance, armor,
  armorToughness, followRange, attackKnockback, scale, regenPerSecond, critChance/critMultiplier,
  immunity flags (fire/fall/drown/knockback), per-cause damage multipliers (elemental resist/weak).
- **Equipment** (`Equipment` of `ItemSpec`): hands + 4 armor slots, per-slot drop chance.
- **AI** (`AiProfile`): aggression, target priority list, follow/leash range, movement style,
  retreat HP %, reinforcement calls, day/night reaction. Implemented via attributes + a throttled
  controller (see §8) — **no NMS goal injection** (deliberate: version-resilient & maintainable).
- **Skills**: ordered list of `SkillDefinition` (skill id + params + trigger rules).
- **Phases** (`Phase`): HP-threshold-driven stat multipliers / skill toggles / messages.
- **Drops** (`DropTable`): item entries (ItemSpec + chance + amount range) + XP range.
- **Presentation**: glow color, bossbar (for elites/bosses), ambient particles/sounds, scale.
- **Scaling hooks**: which stats scale with player count / difficulty and how hard.

Built by `MobParser` from a `ConfigSection`, returning `(Optional<MobDefinition>, ValidationReport)`.
Stored in `MobRegistry` by id.

## 5. Skill execution engine

- `Skill` is a stateless strategy: `cast(SkillContext)`. Registered in `SkillRegistry` by id.
- `SkillDefinition` = skill id + a params `ConfigSection` + a `TriggerSpec`
  (cooldown, warmup, chance, range, radius, duration, target rule, phase restriction, conditions).
- `SkillInstance` binds a definition to one `ActiveMob` and owns the live cooldown/warmup state.
- New skills are added by implementing `Skill` + registering — **no core edits**. Skills read all
  tunables from their params section, so behaviour is data-driven and overridable per-mob & per-horde.
- Classification (`SkillType`: OFFENSE/DEFENSE/MOVEMENT/SUMMON/CONTROL/UTILITY) drives defaults and docs.
- Generic skills cover many config options with one class (e.g. one `EffectSkill` handles
  poison/weakness/slow/blind/wither/burn/levitation; one `ProjectileSkill` handles
  fireball/wither-skull/snowball/arrow bursts).

## 6. Horde composition + runtime engine

- `HordeDefinition`: identity, difficulty, duration, cooldown, activation conditions, `AreaSpec`,
  world/biome/time gating, player count gating, scaling, ordered `Wave`s, optional final boss,
  rewards, presentation, spawn rules, anti-abuse.
- `Wave` → list of `WaveEntry` (mobId, count range, weight, batch, elite/miniboss flag,
  per-mob overrides). Advance condition: % cleared and/or timer.
- `HordeManager` owns active `HordeInstance`s, global cooldowns, capacity caps, trigger checks.
- `HordeInstance` is a tick-driven state machine: PREPARE → wave loop (SPAWNING → FIGHTING →
  CLEARED) → BOSS → COMPLETE / FAILED / TIMEOUT. Spawns are **batched** across ticks by
  `HordeScheduler` to avoid spikes.

## 7. Spawn area engine

- `Shape` (SPHERE/CYLINDER/CUBE) is pure math: `randomLocalPoint(rng)` + `contains(dx,dy,dz)`.
- `AreaSpec` binds shape + center (fixed coords *or* dynamic around trigger/players) + world.
- `SpawnFinder` turns a candidate point into a **safe** `Location`: chunk-loaded check, solid-block
  avoidance, ground search, min-distance-from-player, density cap, attempt budget. Fails safe
  (returns empty) rather than spawning illegally.

## 8. Performance & safety model

- **One throttled controller task** ticks all active mobs every N ticks (default 5–10), not every
  tick, and does bounded work per tick (skill checks, target refresh, AI nudges). No per-entity
  `BukkitRunnable` swarm.
- Horde spawns **batched** (`maxSpawnsPerTick`) and capped (`maxActiveMobs`, `maxConcurrentHordes`).
- Definitions parsed **once** at load and cached (immutable). Reload re-reads files atomically into
  a new registry, swapped in only if it parses (corrupt config never replaces a good registry wholesale).
- Custom mobs tagged via **PDC** (`util/Keys`): survive chunk unload (persistent, `removeWhenFarAway=false`),
  rebind on chunk load, and orphan cleanup removes leftovers on disable/reload.
- Skills wrapped in try/catch — a failing skill logs once and never breaks the tick loop.
- Double-spawn / race guards: spawn goes through one main-thread path; instance sets are `Set<UUID>`.
- Fail-safe defaults: every missing field has a documented default; impossible configs are rejected
  with a clear reason and skipped (the rest still load).

## 9. Validation & error reporting

`ValidationReport` collects `Issue`s at three levels:
- **ERROR** — definition cannot load (rejected, reason given).
- **WARN** — loaded with a corrected/defaulted value (told what changed).
- **INFO** — note (e.g. deprecated key alias accepted).

`/am validate` runs the whole pipeline dry and prints a per-file summary: valid / corrected / rejected.

## 10. Commands & permissions

Root `/amazingmobs` (alias `/am`) with a `SubCommand` registry (clean dispatch + tab completion).
Subcommands: `help reload list info create edit set save export delete spawn test give
start stop status validate debug`. Permissions are granular (`amazingmobs.admin`,
`amazingmobs.mob.create`, `amazingmobs.horde.start`, `amazingmobs.debug`, ...). See `docs/PERMISSIONS.md`.

## 11. Public API & events

`AmazingMobsApi` exposes registries + horde control. Bukkit events fire for integration:
`HordeStartEvent`, `HordeStopEvent`, `WaveCompleteEvent`, `CustomMobSpawnEvent`,
`CustomMobDeathEvent`, `SkillTriggerEvent`, `BossPhaseChangeEvent`.

## 12. Deliberate scope boundaries (residual limits)

- **AI uses attributes + a throttled controller, not NMS goal rewrites.** This trades pixel-perfect
  vanilla pathing nuance for cross-version stability and zero NMS coupling — the right call for a
  content plugin meant to survive Paper updates.
- **No external runtime dependencies** beyond `paper-api` (provided). Smaller jar, fewer breakages.
- Example content is balanced by design intent and internal consistency, not by live playtest
  telemetry (which only a real server provides) — values are sane starting points, clearly tunable.
