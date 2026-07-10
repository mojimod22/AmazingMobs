# Trait Reference (modular affixes)

Traits are reusable **behaviour modules** layered onto any mob via a `traits:` list — they hook the
mob lifecycle (spawn / tick / damaged / attack / death) rather than being cast like skills. Combine
freely to give a mob personality and tactics. Add via either form:

```yaml
traits: [berserker, thorns]                     # id-only
# or, with params:
traits:
  - { trait: berserker, threshold: 0.4, strength: 1 }
  - { trait: hunter, drive: true, drive-power: 0.3, reach: 2.5 }
```

Unknown trait ids are warned and skipped. `/am list traits` shows everything registered.

> **Making goal-less mobs fight.** Villagers, chickens, cows and bats have no vanilla attack goal.
> Give them `hunter` (`drive: true`) to close distance + the `strike` skill to land hits — that combo
> turns *any* entity into a real melee combatant without NMS.

## Behaviour

| id | what it does | key params |
|---|---|---|
| `berserker` | sustained STRENGTH/SPEED below a health threshold | `threshold` (0.5), `strength` (1), `speed` |
| `cowardly` | flees the nearest player when wounded | `flee-at` (0.4), `speed-boost` |
| `hunter` | relentlessly re-targets nearest player; `drive` nudges movement | `range` (48), `speed`, `drive`, `drive-power` (0.25), `reach` (2.2) |
| `leaper` | periodic pounce at the target | `cooldown` (4s), `power` (1.3), `upward`, `range` |
| `ambusher` / `burrower` | invisible until it strikes/is struck, then reveals | `reveal-strength` |
| `mimic` | hides name (looks harmless) until provoked | `reveal-message` |
| `teleporter` / `phase_walker` | periodic short blink (phase_walker adds resistance) | `cooldown` (5s), `mode`, `distance`, `resistance` |
| `revenge` | locks onto + empowers vs attacker; `rally` sics allies | `strength` (1), `rally`, `rally-radius` |
| `vampire` / `parasite` | heals on hit (parasite also weakens) | `heal` (2), `weaken`, `weaken-duration` |
| `thorns` | reflects flat damage to melee attackers | `amount` (3) |
| `exploder` | detonates on death (AoE + knockback, optional fire/block-damage) | `radius` (4), `damage` (8), `knockback`, `fire`, `block-damage` |
| `pack` / `swarm_leader` | STRENGTH scaling with nearby same-type allies | `radius` (10), `per` (0.34), `max-amplifier` (3) |
| `fake_death` | "dies" then comes back enraged once below a threshold | `threshold` (0.3), `delay` (50t), `heal`, `message` |
| `enrage` | one-shot big buff + shout below a threshold | `threshold` (0.35), `duration` (30s), `strength`, `speed`, `resistance`, `message` |
| `carrier` | hardens whatever rides it (pairs with mounts/stacks) | `resistance`, `regeneration`, `speed` |

## Auras (synergy / control)
Periodic potion auras. Target set depends on the id; config can override `effects`, `radius`, `cooldown`.

| id | affects | default effect(s) |
|---|---|---|
| `buffer` | allies | strength + speed |
| `commander` | allies | strength + speed + resistance |
| `protector` / `guardian` | allies | resistance (guardian II) |
| `ritualist` | allies | strength + regeneration |
| `healer` | allies + self | heals (`amount`, `percent`) |
| `regenerator` | self | heals self |
| `saboteur` | players | mining fatigue + weakness |
| `disruptor` / `controller` | players | slowness + weakness / mining fatigue |
| `hexer` | players | poison + weakness |
| `frost_aura` | players | slowness II |
| `siege` | players | slowness (immovable bruiser flavour) |

Aura params: `radius` (10), `cooldown` (3s), `duration` (4s), `same-type-only`, or an explicit
`effects: [{type, amplifier, duration}]` list.

## Extending
Implement `eu.breakapp.amazingmobs.trait.Trait` (or extend `AbstractTrait`), register in
`TraitRegistry`. Usable from any mob's `traits:` immediately — no core edits.
