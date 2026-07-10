# AmazingMobs

> A large-scale, fully configurable **horde event engine** and **custom-mob system** for **Paper 1.21.11**.

AmazingMobs lets you run intense, spectacular, *balanceable* mob events across wide areas — from a
gentle early-game skirmish to a 16-minute, triple-boss server finale — while authoring everything in
clean YAML or live in-game with commands. It ships with **53 example mobs** and **41 example hordes**
that work out of the box.

- ✅ Verified to load on a real Paper 1.21.11 server (0 errors).
- ✅ Zero runtime dependencies beyond the Paper API.
- ✅ Data-driven, extensible **skill + trait** engines (add either without touching the core).
- ✅ Emergent depth: modular traits, spawn-time **variants/mutations**, **mounts & stacks**,
  **mob-as-projectile**, and an adaptive **director**.
- ✅ **Player classes** (1.6.0): 6 distinct classes (Necromancer, Stormcaller, Pyromancer, Vanguard,
  Assassin, Guardian) with active / special / hyper skills, cooldowns, a skill GUI, and prestige scaling.
  See **[docs/CLASSES.md](docs/CLASSES.md)**.
- ✅ **Weight progression** (1.5.0): a universal, persistent RPG stat — kill mobs to gain hearts,
  strength & speed, then `/am prestige` for permanent tiers. See **[docs/PROGRESSION.md](docs/PROGRESSION.md)**.
- ✅ **Arena flow** (1.5.0): `/am setarenapos` + `/am tpall` gather players safely on the floor, and
  horde start auto-heals & kits everyone.
- ✅ Performance-conscious: one throttled controller, batched spawns, hard caps, PDC rebinding.

---

## Highlights

| Area | What you get |
|---|---|
| **Custom mobs** | identity, stats, gear, AI profile, data-driven skills, HP phases, custom drops, presentation (glow / boss bar / FX), per-player & difficulty scaling |
| **Traits** | ~30 modular affixes (berserker, hunter, exploder, vampire, ambusher, mimic, fake-death, enrage, pack, carrier, and synergy auras) — combine freely; goal-less mobs (villagers/chickens/bats) fight via `hunter`+`strike` |
| **Variants** | spawn-time mutations (fire/ice/thunder/corrupted/royal/…) gated by biome/time/weather — one base mob, a whole mutation tree |
| **Mounts & stacks** | a mob riding another, or a vertical column of mobs, with cascading death chains; plus `launch_mob` to hurl live mobs as projectiles |
| **Director** | opt-in adaptive intensity that scales spawns to how the fight is going |
| **Skills** | 37 ids: fireballs, wither skulls, snowballs, thunder, dash, blink, jump-slam, area slam, repel/pull, summon, heal, shield, rage, fear, vanish, flight, web traps, strike, orbit, launch-mob, and every status effect |
| **Hordes** | wave-by-wave or phase-by-phase composition, activation gating (world/biome/time/players), spherical/cylindrical/cubic areas (~up to 1024-block radius), scaling, rewards, boss bars, anti-abuse cooldowns |
| **Authoring** | two ways: official YAML files (`/mobs`, `/hordes`) **and** in-game commands (`/am create … set … save`) |
| **Classes** | *(1.6.0)* 6 classes (Necromancer/Stormcaller/Pyromancer/Vanguard/Assassin/Guardian), each with active/special/hyper skills + passive; F / Shift+F / Shift+Q triggers; skill GUI; prestige scaling |
| **Progression** | *(1.5.0)* universal **Weight** stat → hearts/strength/speed, `/am prestige` for permanent tiers, anti-park decay, live sidebar |
| **Arena & prep** | *(1.5.0)* `/am setarenapos` + `/am tpall` (safe grounded placement); auto heal + battle-kit on horde start |
| **Ops** | granular permissions, validation with clear errors, hot reload, dry-run validate, diagnostics, public events + API |

---

## Requirements

- **Paper 1.21.11** (artifact `io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT`)
- **Java 21+**

## Install

1. Drop `AmazingMobs.jar` into your server's `plugins/` folder.
2. Start the server. On first run it creates `plugins/AmazingMobs/` with `config.yml`, `messages.yml`,
   and the bundled `mobs/` + `hordes/` examples.
3. `/am list mobs`, `/am spawn dread_overlord`, `/am start blood_moon force` — and you're away.

## Build

**Canonical (Maven):**
```bash
mvn -f plugin/pom.xml package      # -> plugin/target/AmazingMobs.jar
```

**No build tool? (bundled scripts, compile against the server's own jars):**
```bash
bash plugin/tools/build.sh --deploy   # builds + copies into ../plugins/
bash plugin/tools/run-tests.sh        # no-dependency verifier (pure logic + all examples)
```
> The scripts exist so the plugin can be built/verified inside a Paper server folder that has no
> Maven/Gradle. Canonical builds and the JUnit suite still use Maven.

---

## Quick start

```
/am help                         # all commands you can run
/am list mobs|hordes|skills
/am info mob dread_overlord
/am spawn ember_magus 3          # spawn 3 where you look
/am test bonecrusher_juggernaut  # spawn one + print resolved stats
/am start night_assault          # start a horde (subject to gating)
/am start blood_moon force       # bypass time/biome/cooldown/player gating
/am status                       # active hordes + runtime counts
/am stop all
/am reload                       # hot-reload config + definitions
/am validate                     # dry-run validate every file
```

In-game mob authoring (no files needed):
```
/am create my_boss wither_skeleton
/am set name <gradient:#f00:#ff0>My Boss</gradient>
/am set stats.health 240
/am set stats.damage 12
/am set presentation.boss-bar true
/am save                         # writes mobs/my_boss.yml and loads it
```

---

## Configuration

```
plugins/AmazingMobs/
├─ config.yml        # performance knobs (controller cadence, caps)
├─ messages.yml      # command prefix (MiniMessage)
├─ mobs/*.yml        # one custom mob per file
├─ hordes/*.yml      # one horde per file
├─ arenas/           # (reserved; areas are defined inline in hordes)
├─ drops/            # (reserved for future shared drop tables)
└─ exports/          # /am export drops portable copies here
```

### Why YAML (and not JSON)?
YAML is the **single official format**. It is the idiomatic Paper choice, supports comments (vital for
a balance-heavy content pack), and uses the parser the server already bundles. JSON was evaluated and
**deliberately not added** — a second hand-authored format would duplicate the schema and validator for
no real gain. `/am export` round-trips through the *same* YAML pipeline, so there is exactly one schema,
one validator, one loader. See [docs/DESIGN.md](docs/DESIGN.md) §2 for the full rationale.

---

## Documentation

- **[docs/DESIGN.md](docs/DESIGN.md)** — architecture, module map, engineering & safety model, residual limits
- **[docs/MOBS.md](docs/MOBS.md)** — complete mob YAML reference + worked example
- **[docs/TRAITS.md](docs/TRAITS.md)** — every trait/affix, its params, and how to combine them
- **[docs/ADVANCED.md](docs/ADVANCED.md)** — variants/mutations, mounts, stacks, mob-as-projectile, director
- **[docs/HORDES.md](docs/HORDES.md)** — complete horde YAML reference + worked example
- **[docs/SKILLS.md](docs/SKILLS.md)** — every skill id, its params, and trigger options
- **[docs/COMMANDS.md](docs/COMMANDS.md)** — every command with examples
- **[docs/CLASSES.md](docs/CLASSES.md)** — player classes, skills, triggers, prestige scaling
- **[docs/PROGRESSION.md](docs/PROGRESSION.md)** — the Weight/Prestige system, bonuses, and config
- **[docs/PERMISSIONS.md](docs/PERMISSIONS.md)** — every permission node
- **[docs/ROADMAP.md](docs/ROADMAP.md)** — build state checklist
- **[CHANGELOG.md](CHANGELOG.md)**

---

## For developers

Obtain the API:
```java
AmazingMobs am = (AmazingMobs) Bukkit.getPluginManager().getPlugin("AmazingMobs");
LivingEntity boss = am.api().spawnMob("dread_overlord", location);
am.api().startHorde("blood_moon", center, /*playerCount*/ 0);
```

Bukkit events you can listen to (`eu.breakapp.amazingmobs.api.event`):
`HordeStartEvent` (cancellable), `HordeStopEvent`, `WaveCompleteEvent`, `CustomMobSpawnEvent`,
`CustomMobDeathEvent`, `SkillTriggerEvent`, `BossPhaseChangeEvent`.

Add a skill without editing the core: implement `eu.breakapp.amazingmobs.skill.Skill` and register it
in `SkillRegistry`. It is immediately usable from any mob's `skills:` list.

---

## Performance & safety

- A single throttled task ticks all custom mobs (default every 8 ticks) with bounded work — no
  per-entity runnables.
- Horde spawns are batched per tick and capped (`max-mobs`, `max-per-tick`, `max-concurrent-hordes`).
- Definitions are parsed once and cached immutably; reload builds a new set atomically.
- Custom mobs are PDC-tagged: they survive chunk unloads, rebind on load, and are cleaned up on stop.
- Every skill is wrapped — a failing skill logs once and never breaks the tick loop.
- Corrupt or impossible config is rejected per-file with a clear reason; the rest still loads.

## Residual limits (by design)
- AI is **controller-driven on top of the real navigator** (`Pathfinder.moveTo` + `lookAt`, with
  vanilla goal-stripping for passive bases) rather than custom NMS goals — chosen for cross-version
  stability. The result is smooth pursuit/facing/anti-stuck; it does not replicate every nuance of
  bespoke NMS goals, but the gap is no longer perceptible in normal play.
- Battlefield objects are realised as **objective entities** (spawner totems / anchors / nests with an
  `objective: true` wave gate), not destructible *blocks* — deliberately, so they reuse the mob, AI,
  boss-bar and reward systems and never grief the world.
- Example balance values are sane, internally-consistent starting points, not live-telemetry-tuned.
- Live combat/movement is best confirmed in-world (a console can't be chased); use `/am give-trait`,
  `/am give-skill`, `/am variant`, `/am debug` (shows target/distance/path) to spot-test.

Made for BreakApp · `eu.breakapp.amazingmobs`
