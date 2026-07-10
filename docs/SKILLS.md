# Skill Reference

Skills are referenced by `skill:` in a mob's `skills:` list. Every skill shares the common `trigger`
block (see [MOBS.md](MOBS.md#skills)); the `params` differ per skill and are listed below. All
particle/sound/effect names are resolved by registry name (e.g. `flame`, `entity_blaze_shoot`,
`poison`) — an unknown name is a safe no-op. **34 ids** are registered out of the box.

Common feedback params accepted by most skills: `sound`, `sound-volume`, `sound-pitch`, `particle`,
`particle-count`.

## Offense — projectiles
`fireball` · `wither_skull` · `snowball` · `arrow_volley` · `projectile_burst` · `dragon_fireball`
(all backed by one engine; they differ only in default projectile `type`).

| param | default | notes |
|---|---|---|
| `type` | per-id | `small_fireball large_fireball/fireball wither_skull dragon_fireball snowball arrow` |
| `count` | 1 | projectiles per cast |
| `speed` | 1.25 | velocity |
| `spread` | 0.08 | random cone spread |
| `yield` | 1.0 | explosion power (fireballs) |
| `incendiary` | false | sets fires (fireballs) |
| `charged` | false | charged wither skull |
| `damage` | 0 | arrow damage override |

## Offense / movement
- **`strike`** *(1.1.0)* — controller-driven melee bite, so goal-less mobs (villagers/chickens/bats)
  can hit. `params: damage` (default = mob's attack damage), `knockback`; trigger `max-range` = reach.
- **`launch_mob`** *(1.1.0)* — hurl a live mob as a projectile with impact callbacks. See
  [ADVANCED.md](ADVANCED.md#mob-as-projectile-launch_mob-skill).
- **`orbit`** *(1.1.0)* — tangential nudge around the target; fired on TICK it makes flyers swarm.
  `params: speed` (0.55), `pull` (0.12), `hover`.
- **`thunder`** — lightning on targets. `params: damage` (extra), `effect-only` (cosmetic strike).
- **`dash`** — lunge at target. `params: power, upward`.
- **`jump_attack`** — leap then AoE slam. `params: power, upward, delay, damage, knockup` + trigger `radius`.
- **`area_slam`** — instant AoE around caster. `params: damage, knockup, knockback` + trigger `radius`.

## Movement / utility
- **`teleport`**, **`blink`** — `params: mode` (`to_target behind_target away_from_target random_near`), `distance`.
- **`flight`** — lift off. `params: amplifier` (levitation), `glide` (adds slow-fall), `approach` (nudge toward target).
- **`vanish`** — invisibility for the trigger `duration`. `params: amplifier`.

## Control
- **`repel`** — knock targets away. `params: power, upward`.
- **`pull`** — drag targets in (same engine, inverted). `params: power, upward, pull`.
- **`trap`** — place temporary blocks (default cobweb) on targets, auto-reverted. `params: material, radius` + trigger `duration`.

## Summon
- **`summon`** — spawn custom mobs. `params: mob` (defaults to a copy of the caster), `count` (number or range), `spread`.

## Defense / status — `effect` engine
One engine backs every status id. `target` (from the trigger) decides who is affected — `SELF` to
buff itself, `ALL_IN_RADIUS` to buff allies, `ALL_PLAYERS_IN_RADIUS`/`TARGET` to debuff players.

Preset ids and their default effects:

| id | type | default effect(s) |
|---|---|---|
| `effect` | CONTROL | *(none — fully config-driven)* |
| `poison` | CONTROL | poison |
| `weakness` | CONTROL | weakness |
| `slow` | CONTROL | slowness II |
| `blind` | CONTROL | blindness + darkness |
| `wither` | CONTROL | wither II |
| `levitate` | CONTROL | levitation |
| `fear` | CONTROL | nausea + slowness II + blindness + darkness |
| `ignite` | OFFENSE | sets target on fire |
| `rage` | UTILITY | strength II + speed II *(use `target: SELF`)* |
| `shield` | DEFENSE | resistance IV + absorption II *(use `target: SELF`)* |
| `buff` | UTILITY | strength + resistance |
| `strengthen` / `hasten` / `regenerate` | UTILITY/DEFENSE | strength II / speed II / regeneration II |
| `heal` | DEFENSE | (not potion) heals — `params: amount`, `percent` |

`effect`-engine params:
```yaml
params:
  effects:                     # explicit list overrides the preset
    - { type: slowness, amplifier: 2, duration: 5s }
    - { type: poison, amplifier: 0 }
  # or a single override:
  type: weakness
  amplifier: 1
  fire-ticks: 80               # also set the target on fire
```

## Triggers recap
`TICK` (periodic, gated by cooldown/range/chance), `ON_DAMAGED`, `ON_ATTACK`, `ON_SPAWN` (once),
`ON_DEATH` (once — e.g. death summons), `ON_LOW_HEALTH` (once, when health first drops below
`max-health-pct`). A skill with multiple `types` fires on any of them.

> **Counterattacks**, **on-death effects**, **rage/enrage** and **phase changes** are all expressed
> with these triggers — e.g. an `ON_DAMAGED` `repel`, an `ON_DEATH` `summon`, an `ON_LOW_HEALTH` `rage`.

## Extending
Implement `eu.breakapp.amazingmobs.skill.Skill` (or extend `AbstractSkill`) and register it in
`SkillRegistry`. Read tunables from `ctx.params()`; it is then usable from any mob immediately — the
core needs no changes.
