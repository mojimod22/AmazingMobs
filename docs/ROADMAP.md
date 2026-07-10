# AmazingMobs — Build Roadmap & State

Living checklist. `[x]` done · `[~]` in progress · `[ ]` todo.

## Phase 1 — Design & scaffolding
- [x] Analyze server folder, confirm paper-api 1.21.11 + bundled libs
- [x] Lock decisions (YAML official format, no-NMS AI, Maven + in-env build)
- [x] DESIGN.md
- [x] pom.xml + build.sh + run-tests.sh + plugin.yml

## Phase 2 — Core foundation
- [x] util: Keys, Text, Numbers, Rng, Cooldown, Schedulers
- [x] config: ConfigSection, ConfigSource, validation (Issue/Level/ValidationReport), PluginConfig, Messages

## Phase 3 — Mob model + parsing
- [x] mob model: Tier, StatBlock, ItemSpec, Equipment, AiProfile, Phase, DropTable, MobDefinition
- [x] mob: MobParser, MobRegistry

## Phase 4 — Skill engine
- [x] skill: Skill, SkillType, SkillContext, TriggerSpec, SkillDefinition, SkillInstance, AbstractSkill, SkillRegistry
- [x] skill.impl: projectiles, control/effects, movement, summon, defense, utility (24 skills)

## Phase 5 — Runtime (mobs)
- [x] ActiveMob, MobSpawner, MobManager (throttled controller, PDC tagging, cleanup)
- [x] listeners: CombatListener, MobLifecycleListener, WorldBindListener

## Phase 6 — Area + horde engine
- [x] area: Shape (pure), AreaSpec, SpawnFinder
- [x] scaling: Scaling (pure)
- [x] horde model: WaveEntry, Wave, HordeDefinition, HordeParser, HordeRegistry
- [x] horde.runtime: HordeScheduler, HordeInstance, HordeManager

## Phase 7 — Commands + API
- [x] command: SubCommand, AmazingMobsCommand (dispatch + tab complete)
- [x] subcommands: help reload list info create edit set save export delete spawn test give start stop status validate debug
- [x] api: AmazingMobsApi + 7 events

## Phase 8 — Bootstrap
- [x] AmazingMobs main (wiring, lifecycle, default-file extraction)
- [x] resources: config.yml, messages.yml

## Phase 9 — Example content
- [x] 20 example mobs (mobs/*.yml)
- [x] 30 example hordes (hordes/*.yml)
- [x] example arena(s)

## Phase 10 — Tests
- [x] JUnit suite (parser, validator, area, scaling, numbers, rng)
- [x] in-env no-dep verifier harness
- [x] full compile green + harness green

## Phase 11 — Docs
- [x] README, MOBS.md, HORDES.md, COMMANDS.md, PERMISSIONS.md, SKILLS.md, CHANGELOG.md

## Phase 12 — Polish
- [x] final compile, validate examples load, review pass

## Phase 2 (1.1.0) — Expansion
- [x] Trait engine (`trait/*`, ~30 ids) wired into ActiveMob lifecycle
- [x] Variant/mutation system (overlays + biome/time/weather conditions + weighted roll)
- [x] Rider system: mounts + stacks/columns + cascading death chains
- [x] Mob-as-projectile (`launch_mob`) + `strike` (controller melee) + `orbit`
- [x] Director (adaptive horde intensity) + pause/freeze
- [x] Aggressive vanilla mobs (interact-cancel; villagers/chickens/cows/bats via hunter-drive+strike)
- [x] Config versioning
- [x] Commands: variant, mount, stack, freeze/unfreeze, list traits, debug composition
- [x] Content: +16 mobs (40 total), +8 hordes (38 total) — villagers, bats, chicken apocalypse, mounts, stacks, projectile
- [x] Tests: JUnit 24, verifier 493 (trait/variant validation)
- [x] Docs: TRAITS.md, ADVANCED.md + updates; CHANGELOG 1.1.0
- [x] Real-server boot verification — 40/40 mobs, 38/38 hordes, 37 skills, 0 errors
