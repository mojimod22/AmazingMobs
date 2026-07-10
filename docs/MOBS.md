# Custom Mob Reference

One mob per file in `plugins/AmazingMobs/mobs/*.yml`. The file name is the default id. All fields are
optional unless marked **required**; missing fields use the documented default. Bad values are
corrected to a default with a `WARN` (see `/am validate`); only a missing/invalid base `type` rejects
the mob. Text fields use [MiniMessage](https://docs.advntr.dev/minimessage). Ranges accept a single
number or `"min-max"`. Durations accept `t` (ticks), `s`, `m`, `h`, or a bare number (seconds).

## Top level

| Key | Type | Default | Notes |
|---|---|---|---|
| `id` | string | file name | lowercased, `[a-z0-9_]` |
| `name` | MiniMessage | = id | display name |
| `lore` | list\<MiniMessage> | – | descriptive lines |
| `type` | entity type | **required** | must be a living entity (e.g. `WITHER_SKELETON`) |
| `tier` | enum | `COMMON` | `COMMON UNCOMMON RARE ELITE MINIBOSS BOSS` (drives colour) |
| `category` | string | `general` | free-form grouping tag |
| `tags` | list\<string> | – | free-form tags |

## `stats`

| Key | Default | Notes |
|---|---|---|
| `health` | 20 | max health (must be > 0) |
| `damage` | 3 | attack damage |
| `speed` | *(vanilla)* | movement speed attribute; omit to keep the entity's default |
| `armor` | 0 | 0–30 |
| `armor-toughness` | 0 | 0–20 |
| `knockback-resistance` | 0 | 0–1 |
| `follow-range` | *(vanilla)* | targeting/aggro range attribute |
| `attack-knockback` | 0 | bonus knockback dealt |
| `scale` | 1.0 | entity size (0.0625–16) |
| `regen-per-second` | 0 | custom health regen (HP/s) |
| `crit-chance` | 0 | 0–1 chance its melee/skills crit |
| `crit-multiplier` | 1.5 | damage ×multiplier on a crit |
| `max-absorption` | 0 | bonus absorption hearts |
| `fire-immune` | false | cancels FIRE/LAVA/etc. |
| `fall-immune` | false | cancels fall damage |
| `drown-immune` | false | cancels drowning |
| `knockback-immune` | false | hard knockback immunity (also sets resistance 1.0) |
| `damage-multipliers` | – | map of *cause/category → multiplier* taken; `<1` resist, `>1` weak, `0` immune |

`damage-multipliers` keys may be a specific cause (`entity_attack`, `projectile`, `magic`,
`wither`, `fall`, `freeze`, …) or a category (`fire`, `explosion`, `projectile`, `magic`, `melee`,
`fall`, `freeze`) or `all`. Most specific wins.

```yaml
stats:
  health: 120
  damage: 9
  armor: 8
  knockback-resistance: 0.5
  damage-multipliers:
    fire: 2.0        # double damage from fire (a weakness)
    projectile: 0.8  # resists arrows
```

## `equipment`
Slots: `main-hand`, `off-hand`, `helmet`, `chestplate`, `leggings`, `boots`. Each is an **item spec**:

| Key | Default | Notes |
|---|---|---|
| `material` | **required** | e.g. `NETHERITE_SWORD` |
| `amount` | 1 | range allowed |
| `name` | – | MiniMessage |
| `lore` | – | list\<MiniMessage> |
| `enchants` | – | map `enchant: level` (e.g. `sharpness: 4`) |
| `unbreakable` | false | |
| `glow` | false | enchant-glint without an enchant |
| `custom-model-data` | – | integer (resource packs) |
| `drop-chance` | 0.085 | 0–1 chance to drop on death |

## `ai`

| Key | Default | Notes |
|---|---|---|
| `aggression` | `AGGRESSIVE` | `PASSIVE DEFENSIVE AGGRESSIVE` |
| `movement` | `CHASE` | `CHASE KITE STRAFE AMBUSH STATIONARY` (flavour for telegraphing) |
| `target-priority` | `[NEAREST]` | list of `NEAREST LOWEST_HEALTH HIGHEST_HEALTH RANDOM MOST_ARMORED LEAST_ARMORED` |
| `target-players-only` | true | |
| `aggro-range` | *(follow range)* | acquisition range |
| `leash-range` | 0 | 0 = none; else pulled back toward spawn |
| `retreat-health-pct` | 0 | 0 = never; flees below this HP fraction |
| `kite-distance` | 8 | preferred stand-off distance (KITE/STRAFE) |
| `chase-speed` | 1.15 | navigation speed multiplier when pursuing — raise for fast, menacing mobs |
| `burns-in-day` | false | sunlight burning |
| `reinforcements` | – | `{ enabled, mob, count, cooldown }` |

> **Movement (1.3.0):** aggressive mobs pursue using the entity's **real navigation** (pathfinding +
> look-at), so even normally-passive bases (chickens, cows, villagers) chase smoothly and face you.
> `movement: CHASE` closes in, `KITE` holds range and shoots, `STRAFE` circles, `STATIONARY` holds.
> Free-flyers (bats/phantoms) use smooth velocity steering. Tune speed feel with `chase-speed`.

## `skills`
A list of skill bindings. See **[SKILLS.md](SKILLS.md)** for every skill id + its `params`.

```yaml
skills:
  - skill: fireball          # required: a registered skill id
    label: "Emberbolt"        # optional, for telegraphs/messages
    trigger:
      types: [TICK]           # TICK ON_DAMAGED ON_ATTACK ON_SPAWN ON_DEATH ON_LOW_HEALTH
      cooldown: 3s
      warmup: 0
      chance: 0.9             # 0–1 roll per attempt
      min-range: 6
      max-range: 40           # <=0 = unbounded
      radius: 4               # AoE radius for area skills/targeting
      duration: 3s            # effect duration
      target: TARGET          # SELF TARGET NEAREST_PLAYER RANDOM_PLAYER LOWEST_HEALTH_PLAYER ALL_PLAYERS_IN_RADIUS ALL_IN_RADIUS
      phases: []              # restrict to these phase ids (empty = any)
      min-health-pct: 0.0     # caster health window
      max-health-pct: 1.0
    params:                   # skill-specific (see SKILLS.md)
      type: small_fireball
      count: 1
      speed: 1.4
      incendiary: true
```

## `phases`
HP-threshold phases, evaluated high→low. When health drops to/below a threshold the deepest reached
phase activates: stats re-multiply, listed skills toggle, and feedback fires once.

```yaml
phases:
  - id: enraged
    threshold: 0.4           # activates at <=40% health
    damage-mult: 1.4
    speed-mult: 1.2
    defense-mult: 0.9        # multiplies armor
    enable-skills: [meteor]  # turn these on
    disable-skills: []
    message: "<red>It enrages!"
    sound: entity_wither_spawn
    particle: flame
```
Tip: gate a skill to a phase via its `trigger.phases: [enraged]` instead of enable/disable for cleaner control.

## `drops`

```yaml
drops:
  clear-vanilla: false       # replace the entity's vanilla drops entirely
  xp: 25-45
  items:
    - { material: DIAMOND, amount: 1-3, chance: 0.5 }
    - material: NETHERITE_SCRAP
      amount: 1
      chance: 0.1
      name: "<gold>Rare Shard"   # item specs support name/lore/enchants/glow too
```

## `presentation`

| Key | Default | Notes |
|---|---|---|
| `glow` | false | outline glow |
| `glow-color` | – | (informational; vanilla glow colour needs scoreboard teams) |
| `name-visible` | true | always-show name plate |
| `boss-bar` | false | show a boss bar to nearby players |
| `boss-bar-color` | `RED` | `PINK BLUE RED GREEN YELLOW PURPLE WHITE` |
| `boss-bar-title` | = name | MiniMessage |
| `ambient-particle` | – | particle emitted while alive |
| `ambient-sound` | – | occasional ambient sound |

## `scaling`
Linear per-player + difficulty scaling, capped so big servers don't create unkillable mobs.

```yaml
scaling:
  health-per-player: 0.15    # +15% health per extra participant
  damage-per-player: 0.06
  speed-per-player: 0.0
  max-players: 8             # cap participants considered
```

## traits  *(1.1.0)*
Modular behaviour affixes (berserker, hunter, exploder, vampire, auras, ...). Full list + params in
**[TRAITS.md](TRAITS.md)**.
```yaml
traits:
  - { trait: hunter, drive: true, drive-power: 0.3 }
  - { trait: exploder, radius: 4, damage: 8 }
```

## variants  *(1.1.0)*
Spawn-time mutation overlays (fire/ice/corrupted/royal/...) gated by biome/time/weather. See
**[ADVANCED.md](ADVANCED.md#variants--mutations)** and `mobs/apocalypse_chicken.yml`.

## mount / riders  *(1.1.0)*
`mount:` makes this mob ride another; `riders:` stacks mobs on top (column). Cascading death chains.
See **[ADVANCED.md](ADVANCED.md#mounts-a-mob-riding-another-mob)**.

---

See the bundled `mobs/dread_overlord.yml` for a full multi-phase boss, and the 1.1.0 showcases:
`apocalypse_chicken` (variants), `living_totem` (stack), `boneback_lancer` (mount), `catapult_fiend`
(mob-as-projectile), `villager_zealot` (aggressive vanilla mob).
