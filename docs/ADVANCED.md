# Advanced Mechanics — Variants, Mounts, Stacks, Projectile-Mobs, Director

These are the systems added in 1.1.0. All are optional, configurable and backward-compatible.

---

## Variants / Mutations
A mob can define `variants:` — overlays rolled at spawn (weighted, gated by biome/time/weather/world/
Y/players). Each overlay tweaks stats, adds traits/skills/tags, recolours, renames and adds drops.
This is how one base mob becomes fire/ice/thunder/corrupted/royal/etc. (see `mobs/apocalypse_chicken.yml`).

```yaml
variant-base-weight: 2.0          # weight of "no variant" (plain mob); 0 = always mutate
variants:
  - id: fiery
    weight: 1.5                    # relative chance among eligible variants
    name-prefix: "<red>Fiery"      # or full `name:` override
    health-mul: 1.2
    damage-mul: 1.2
    armor-mul: 1.0
    tier: ELITE                    # optional tier override
    glow: true
    glow-color: RED
    ambient-particle: flame
    tags: [fire]
    conditions:                    # all optional; omitted = always eligible
      biomes-allow: [desert, badlands]
      biomes-deny: []
      worlds: [world]
      time-min: 13000
      time-max: 23000
      weather: THUNDER             # ANY | CLEAR | RAIN | THUNDER
      y-min: -64
      y-max: 64
      min-players: 1
    traits: [ ... ]                # added on top of the base mob's traits (same syntax as TRAITS.md)
    skills: [ ... ]                # added skills (same syntax as MOBS.md)
    drops: [ { material: BLAZE_POWDER, amount: 1, chance: 0.3 } ]  # extra drops
```
Test a specific one: `/am variant apocalypse_chicken fiery`.

---

## Mounts (a mob riding another mob)
```yaml
mount:
  mob: bonesteed                   # custom mob id OR vanilla type; spawned as the mount
  on-mount-death: ENRAGE           # KEEP | DROP | KILL | SCATTER | ENRAGE (this rider's fate)
  rider-bonus: [resistance:0]      # effects granted to the rider while mounted
  kill-mount-when-rider-dies: false
```
The mount drives the pair's movement (give the *mount* `hunter` `drive:true`); the rider attacks
(`strike`). See `mobs/boneback_lancer.yml` + `mobs/bonesteed.yml`. Test: `/am mount boneback_lancer bonesteed`.

## Stacks / Columns (mobs piled on one base)
```yaml
riders:                            # a vertical passenger chain on top of this mob
  - { mob: totem_core,  on-base-death: DROP, bonus: [resistance:0] }
  - { mob: totem_crown, on-base-death: KILL }   # cascading death per layer
```
Each layer can be a different role with its own stats/skills/resistances — a tactical *structure*,
not one mob. See `mobs/living_totem.yml`. Test: `/am stack living_totem totem_core totem_crown`
(or just spawn `living_totem`, which assembles itself).

> Cascading death, mount death-chains, depth and self-ride are guarded; chains clean up on stop/reload.

---

## Mob-as-Projectile (`launch_mob` skill)
A skill that hurls a *live mob* at players, with impact callbacks. See `mobs/catapult_fiend.yml`
(throws grunts) and `mobs/chicken_king.yml` (throws chickens).
```yaml
- skill: launch_mob
  trigger: { types: [TICK], cooldown: 5s, max-range: 44, radius: 3, duration: 6s }  # radius=impact, duration=TTL
  params:
    mob: zombie_grunt
    speed: 1.4
    arc: true            # lob in an arc
    homing: false        # weak homing toward players
    gravity: true
    damage: 4            # impact AoE damage
    knockback: 0.6
    consume-on-impact: false   # false => the thrown mob lands and keeps fighting
    impact-summon: spiderling  # optional: spawn N of these on impact
    impact-summon-count: 2
```

---

## Director (adaptive intensity)
Opt-in per horde. After each wave clears, it compares the clear time to a target and nudges a spawn
multiplier within bounds — dominate → harder, struggle → easier. Readable via `/am debug`.
```yaml
director:
  enabled: true
  min-multiplier: 0.7
  max-multiplier: 1.9
  step: 0.15
  target-clear-seconds: 40
```
See `hordes/farm_apocalypse.yml`, `hordes/chicken_extinction.yml`, `hordes/villager_revolt.yml`.

---

## Aggressive vanilla mobs (villagers / chickens / cows / bats)  *(hardened 1.3–1.4)*
Right-click interaction with any custom mob is blocked (no villager trading). Aggression is driven by
the controller's **real navigation** (`Mob.getPathfinder().moveTo` + `lookAt`) — add the `strike`
skill so a goal-less base can land melee. For normally-passive bases (villager/chicken/cow/bat/sheep/
pig/…) the plugin **auto-strips vanilla MOVE/TARGET/LOOK goals** so there is no panic/flee/wander/
erratic-fly fighting the controller — they become clean, relentless pursuers. Force it on any mob with
`ai.clear-vanilla-goals: true`; tune speed with `ai.chase-speed`.

---

## Objective waves (battlefield objects)  *(1.4.0)*
Battlefield objectives are **entity-based** (integrated with the mob/AI/boss/reward systems, no block
grief). Mark a wave entry `objective: true` and the wave clears **only when those entities are
destroyed**, regardless of how many adds remain (adds keep coming until then).
```yaml
- label: "Destroy the Nests!"
  mobs:
    - { mob: bat_nest, count: 2, objective: true }   # must be destroyed to advance
    - { mob: bomber_bat, count: 2-3, per-player: 1 }
```
Bundled objective mobs (stationary, knockback-immune, boss-bar): `spawner_totem` (pumps minions +
buffs the horde), `ritual_anchor` (commands the horde + shulker turret), `bat_nest` (births bats).
See `hordes/hollow_nest_war.yml` and `hordes/siege_of_the_keep.yml`.

---

## In-game list authoring  *(1.4.0)* — no more file edits for lists
Inside an edit session (`/am create` or `/am edit`):
```
/am addskill fireball cooldown=2s target=NEAREST_PLAYER max-range=30 count=3 incendiary=true
/am addtrait berserker threshold=0.4
/am adddrop DIAMOND chance=0.5 amount=1-3
/am rmentry skills 0          # remove the first skill
/am show                      # YAML preview before committing
/am save
```
Keys that belong to a skill trigger (`cooldown warmup chance min-range max-range radius duration target
types phases min-health-pct max-health-pct`) auto-route to the `trigger` block; everything else becomes
`params`. `types`/`phases` accept comma lists.

## Runtime mutation (live spot-testing)  *(1.4.0)*
Inject onto the mob you're looking at, no reload:
```
/am give-trait vampire heal=4
/am give-skill thunder around=true count=5 cooldown=4s
/am variant apocalypse_chicken royal   # force a specific variant spawn
```
