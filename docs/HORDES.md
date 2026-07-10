# Horde Reference

One horde per file in `plugins/AmazingMobs/hordes/*.yml`. A horde is a tick-driven state machine:
`DELAY → SPAWN → FIGHT` per wave, then rewards. Spawns are batched and capped for performance.

## Top level

| Key | Type | Default | Notes |
|---|---|---|---|
| `id` | string | file name | |
| `name` | MiniMessage | = id | |
| `description` | string | – | |
| `difficulty` | number | 1.0 | global multiplier applied to every spawned mob's stats |
| `duration` | duration | 0 | overall time cap (0 = run until cleared) |
| `cooldown` | duration | 5m | global cooldown after it ends |
| `infinite` | bool | false | loop the wave list until the time cap / wipe |
| `director` | section | off | *(1.1.0)* adaptive intensity — see [ADVANCED.md](ADVANCED.md#director-adaptive-intensity) |

## `area`
The spawn/combat zone. Pure geometry bound to a world.

| Key | Default | Notes |
|---|---|---|
| `shape` | `CYLINDER` | `SPHERE CYLINDER CUBE` |
| `radius` | 64 | horizontal half-extent (4–1024) |
| `height` | 32 | full vertical extent (cylinder/cube; sphere uses `2*radius`) |
| `dynamic` | true | `true` = centre on the trigger location / players; `false` = fixed coords |
| `world` | – | required if `dynamic: false` |
| `x` `y` `z` | 0/64/0 | fixed centre coords (when `dynamic: false`) |

```yaml
area: { shape: CYLINDER, radius: 80, height: 40, dynamic: true }
# or a fixed arena:
area: { shape: CYLINDER, radius: 40, height: 20, dynamic: false, world: world, x: 0, y: 64, z: 0 }
```

## `spawn` (spawn rules)

| Key | Default | Notes |
|---|---|---|
| `max-per-tick` | 8 | spawns processed per horde-tick (batching) |
| `max-mobs` | 80 | max alive horde mobs at once (concurrency cap) |
| `min-player-distance` | 8 | never spawn closer than this to a player |
| `attempts` | 24 | placement attempts before giving up on a mob this tick |

`SpawnFinder` only uses already-loaded chunks, requires two passable blocks over solid ground, avoids
liquids, and respects `min-player-distance` — it fails safe rather than spawning illegally.

## `activation` (gating)
Checked on start (admins can bypass with `/am start <id> force`).

| Key | Default | Notes |
|---|---|---|
| `worlds` | any | allow-list of world names |
| `biomes-allow` | any | allow-list of biome keys (e.g. `desert`) |
| `biomes-deny` | none | deny-list |
| `time-min` / `time-max` | any | world-time window 0–24000 (supports wrap, e.g. 13000→23000 = night) |
| `min-players` | 1 | minimum participants in the area |
| `max-players` | 8 | cap used for per-player scaling |

## `waves`
An ordered list. Each wave:

| Key | Default | Notes |
|---|---|---|
| `label` | `Wave N` | shown in the boss bar |
| `start-delay` | 2s | pause before the wave begins spawning |
| `duration` | 0 | force-advance after this long (0 = none) |
| `clear-threshold` | 1.0 | fraction that must be killed to advance (e.g. 0.85 = 85%) |
| `message` / `title` / `subtitle` | – | MiniMessage announced on wave start |
| `sound` | – | played on wave start |
| `mobs` | **required** | list of mob lines (below) |

Each `mobs` entry:

| Key | Default | Notes |
|---|---|---|
| `mob` | **required** | a custom mob id (validated; unknown → warned & skipped) |
| `count` | 1 | base count (range allowed) |
| `per-player` | 0 | extra spawns added per participant beyond the first |
| `chance` | 1.0 | 0–1 chance this line spawns at all (for "special" mobs) |
| `role` | `minion` | PDC tag: `minion` / `elite` / `boss` |
| `boss` | false | marks the wave's boss (use `clear-threshold: 1.0` for boss waves) |

```yaml
waves:
  - label: "First Blood"
    start-delay: 3s
    clear-threshold: 0.9
    title: "<red>Wave 1"
    mobs:
      - { mob: zombie_grunt, count: 8-12, per-player: 3 }
      - { mob: nightshade_stalker, count: 1, per-player: 1, role: elite, chance: 0.5 }
  - label: "The Champion"
    start-delay: 5s
    clear-threshold: 1.0
    title: "<dark_red>The Overlord"
    mobs:
      - { mob: dread_overlord, count: 1, boss: true, role: boss }
```

## `rewards`
Granted to every participant in the area on completion.

```yaml
rewards:
  xp: 100-200
  message: "<green>Victory!"
  commands:                       # run as console; %player% is substituted
    - "eco give %player% 500"
  items:
    - { material: DIAMOND, amount: 2-4, chance: 1.0 }
```

## Presentation (top level)

| Key | Notes |
|---|---|
| `start-message` / `start-title` / `start-subtitle` | announced on start |
| `end-message` | on success |
| `fail-message` | on timeout / forced stop |
| `sound` | played on start |

A boss bar showing `name — Wave x/y (alive)` appears automatically when a start title/message is set.

---

See bundled examples for every theme: `first_blood` (intro), `arena_defense` (fixed area),
`survival_gauntlet` (6 waves), `endurance_trial` (infinite), `blood_moon` / `last_stand` (mega events).
