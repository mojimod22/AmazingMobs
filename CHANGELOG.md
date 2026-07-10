# Changelog

All notable changes to AmazingMobs are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); versions follow SemVer.

## [1.6.0] — Player classes, skills, hyper mechanics

### Class system
- **6 classes**, each a distinct role/fantasy with a passive + **active / special / hyper** skill set on
  top of the universal **AK-47 base**: **Necromancer** (summon/attrition), **Stormcaller** (ranged
  control), **Pyromancer** (area damage), **Vanguard** (tank), **Assassin** (mobility/burst),
  **Guardian** (support).
- **Pick + persist:** `/am class` opens a preview GUI (role, passives, skills); `/am class <id>` picks
  directly. Saved in `classes.yml`, survives restart. Re-pick gated by `classes.allow-changing`.
- **Triggers** (no keybind API, so standard keys are hijacked for classed players only): **F** = active,
  **Shift+F** = special, **Shift+Q** = hyper, right-click the AK-47 = base.
- **Skill menu** (`/am skills`): readable inventory of your skills with type, trigger, **effective
  cooldown at your prestige**, remaining cooldown, and description — click a ready skill to cast.
- **Cooldowns** per-player; **action-bar + sound** feedback on cast / on-cooldown.

### Necromancer (flagship)
- **Soul Harvest passive:** your kills can **raise the slain as permanent, glowing minions** that hunt
  the nearest enemy and follow you when idle (chance 35%→75% and cap scale with prestige). Minions fall
  when you die/quit/change class; orphans swept on restart; minion kills never spawn more (no runaway).
- Skills: **Soul Bolt** (heal-on-kill), **Raise Dead** (summon a pack), **Army of the Damned** (hyper:
  a temporary reaver host + necrotic aura).

### Prestige scaling
- Every skill scales with your **Weight prestige**: −cooldown (to −50%), +damage/duration, +summon
  count, +chain jumps/strikes, +minion cap. The menu shows effective values.
- **Prestiging now resets your class** — banking a Weight Prestige wipes your class (and minions) so you
  pick a fresh one with `/am class`, recombining your build each tier.

### Skill items in the horde kit
- On horde start, players with a class are handed their **active / special / hyper as physical skill
  items** in priority hotbar slots (slot 0 sword, 1 AK-47, 2–4 skills) — **right-click an item to cast**
  it. This trims the potion fill (potions still pack every remaining slot). Sword + AK-47 are given to
  everyone; skill items only to classed players.

### Airburst (always-on combat behaviour)
- Not a skill, no class needed: when a mob's hit **launches you > 5 blocks** above the ground and you
  then **land back on your feet**, a retaliatory blast goes off at the landing spot. Its **size is a
  delta** of your current **Weight × prestige × launch speed × peak height** — bigger when you're
  heavier, higher-prestige, hit harder, or sent higher. It's a **real TNT-grade explosion (power 4→8)
  that craters the terrain** and damages + ignites surrounding mobs — but **every nearby player is made
  briefly invulnerable across the blast, so it never hurts players** (the area takes it, you don't).
  **Precise lightning + fire stay on a safe ring at a distance** — never the player at the centre. Runs
  for every player, horde or not. Config: `airburst.enabled` / `airburst.min-height` /
  `airburst.break-blocks` (set false to keep terrain intact).

### Content
- New mob **Souhail Moutmir** (furious zombie-villager mini-boss: absurd sprint, repeated multi-block
  leaps, periodic blink-away → explosive wither-skull barrage with fire + lightning + poison + slow,
  then leaps back for fast weak melee) and **Giocatrice della Squadra Femminile** (very fast villager
  that dances around you in a swarm, many tiny rapid jabs). New horde **The Souhail Horde**. Now **53
  mobs / 41 hordes**.

### Internal
- **Package renamed** `eu.breakapp` → **`me.zygotecode`** (groupId + all sources + `plugin.yml` main
  class). PDC keys are unchanged (namespaced by plugin name), so existing tagged entities/items and
  saved data still bind — no migration needed.

### Config / docs
- New `classes:` config block (enabled, allow-changing, cooldown-multiplier, minion caps). New
  **[docs/CLASSES.md](docs/CLASSES.md)**.

## [1.5.0] — Weight progression, arena flow & battle-prep

### Weight RPG (universal, persistent)
- New **Weight** stat (kg) per player, persisted in `weights.yml`, surviving restarts and shared across
  all hordes. Everyone starts at **60 kg**; the goal is **175 kg**.
- Weight grants **scaled bonuses** (healthy, never absurd): up to **+4 hearts**, **+3 melee damage**,
  and a **slight** speed boost (~+15% of base walk speed) at the goal. Applied as keyed
  `AttributeModifier`s, so they layer cleanly and are re-applied on join.
- **Kills add weight**, scaled by mob difficulty: common ~0.3 kg → boss **5 kg** (vanilla kills 0.2 kg).
- **`/am prestige`** (at the goal): bank a permanent **Weight Prestige** tier — **+1 heart** and
  **+strength forever** (stacking), then reset to 60 kg to climb again. Starts at prestige 1.
- **Anti-park decay**: sit at the goal for **> 10 min** without prestiging and you slim back to
  **140 kg** (configurable). Counts offline time too (no relog-park abuse).
- **Sidebar scoreboard** ("⚖ WEIGHT") shown to **every player permanently** (not only during hordes):
  current weight, goal, prestige tier — updates live on kills/prestige/decay.
- All knobs in `config.yml` under `weight:` (`enabled`, `base-kg`, `goal-kg`, `decay-to-kg`,
  `decay-minutes`).

### Battle prep on horde start
- When a horde starts, every online player is **full-healed** (oxygen/fire/fall/food/effects reset),
  their **inventory + armor + offhand cleared**, and handed the standard **kit**: enchanted netherite
  armor (Protection IV, Unbreaking III, Mending; boots also Feather Falling IV), a netherite sword in
  slot 1 (Sharpness V, Fire Aspect II, Sweeping Edge III, Knockback II, Unbreaking III, Mending), an
  **enchanted shield** in the off-hand (Protection IV, Unbreaking III, Mending), a **bow** (Infinity I,
  Flame, Power V, Mending) with a single arrow, the **"AK-47"** rifle (see below), **3 full stacks of
  enchanted golden apples**, **full stacks of totems**, and **every remaining slot packed with
  strength/speed/regen potions** (drinkables are max-stack 1, so the whole inventory is filled — no
  empty space).

### "AK-47" custom weapon
- A golden-axe-shaped **semi-automatic rifle** that does **no melee damage** and fires on **right-click**.
- Each shot launches a **charged wither skull** driven along a **fixed straight ray at constant speed**
  (`AkProjectile`) — immune to its own self-acceleration, gravity, and any explosion knockback, so the
  trajectory never warps. On impact it deals heavy direct damage, triggers a **medium block-safe
  explosion** that **ignites the area**, and has a **20% chance to call lightning**.
- **32 rounds**; when emptied it **auto-reloads after 60 seconds**. Remaining/total ammo is shown live
  in the weapon's **name and lore** (and on the action bar as you fire).
- Server-wide for now; toggle with `horde.equip-all-players-on-start` (default `true`).

### Arena flow
- **`/am setarenapos`** — set the single global arena spawn (persisted to `arena.yml`).
- **`/am tpall`** — teleport all online players into the arena, each **snapped to a safe grounded
  spot** near the spawn (never in a wall, in the air, or underground).

### Fixes
- **Restart-no-spawn bug fixed.** Restarting a horde (same one, or after finishing/interrupting)
  could spawn **zero** mobs: spawn candidates were placed around the players' centroid but validated
  against the fixed arena centre, so if players had wandered outside the bounds *every* candidate was
  rejected and the SPAWN phase hung forever. The ring origin now **clamps back to the arena centre**
  when the centroid is out of bounds, with a **centre fallback** per spawn and a **10s anti-stall**
  safety net so a wave can never hang.
- **Kamikaze Chicken knockback reduced** (0.9 → 0.45) — easier to face while keeping the threat.

## [1.4.0] — residual-limits remediation / hardening

### AI realism (C, D, A — keystone)
- **Vanilla goal stripping.** Normally-passive bases made aggressive (villager, chicken, cow, bat,
  sheep, pig, etc.) now have their vanilla MOVE/TARGET/LOOK goals removed via Paper's `MobGoals`, so
  there is **no more panic/flee/wander/erratic-fly fighting the controller** — they're clean puppets
  of our pathfinding pursuit. Hostiles/golems keep their good combat AI. Opt-in flag
  `ai.clear-vanilla-goals` too. Villagers are now relentless, not "offended merchants".
- **Anti-stuck recovery**: a mob making no progress toward a far target jumps the lip + force-repaths
  (fixes tight-terrain stalls).

### Bats (G) — full rework
- Bats are now a real threat: goal-stripped + steered so they actually fly at and bite players.
- New bats: `swarm_bat` (fast fodder), `bomber_bat` (dives + detonates), `bat_broodmother` (mini-boss
  that births swarms, shrieks fear, blinds). `bat_eclipse` rebuilt; new objective horde `hollow_nest_war`.

### Battlefield objects (E) — objective-wave system
- Entity-based, integrated with hordes/AI/rewards (no block-grief). A wave entry marked
  `objective: true` makes the wave clear **only when those objectives are destroyed**. New objective
  mobs: `spawner_totem` (pumps minions + buffs horde), `ritual_anchor` (commands the horde, shulker
  turret), `bat_nest`. Used in `hollow_nest_war` + `siege_of_the_keep`.

### Authoring (B) + runtime tooling (F)
- In-game **list authoring** (no file edits): `/am addskill <id> key=val…`, `/am addtrait`,
  `/am adddrop`, `/am rmentry <listPath> <index>`, `/am show` (YAML preview). Trigger keys auto-route
  to the trigger block.
- **Runtime mutation**: `/am give-trait <id> [params]` and `/am give-skill <id> key=val…` inject onto
  the live mob you look at — spot-test combos without editing files.

### Cleanup
- Edit sessions cleared on player quit (no stale sessions). Goal-clear/steering are version-safe
  (wrapped); link maps + tasks already cleaned on stop/reload.

## [1.3.0] — combat & AI audit / rework

### Movement & pursuit (root-cause rework)
- **Aggressive mobs now move properly.** Replaced the per-tick `setVelocity` nudge + bare `setTarget`
  (which did nothing for goal-less mobs like chickens/cows/villagers and looked jittery/"dragged")
  with the entity's **real navigation**: `Mob.getPathfinder().moveTo(target, chaseSpeed)` + `lookAt`.
  Mobs now face the player, path smoothly, climb stairs, and engage with intent.
- Per-`AiProfile.movement` behaviour: `CHASE`/`AMBUSH` close in; `KITE` holds range and shoots;
  `STRAFE` circles; `STATIONARY` holds. Free-flyers (bats/phantoms/ghasts/vexes) use smooth blended
  velocity steering (they ignore the ground navigator).
- **Sticky targeting** (anti flip-flop), **re-path throttling** (only when the target moves / path is
  lost), and `chase-speed` (new `ai.chase-speed`, default 1.15) for per-mob speed feel.
- Controller cadence raised to every **5 ticks** (was 8) for responsive pursuit.

### Combat cadence
- Mobs now apply **continuous pressure**: every ready TICK skill may fire each pass (each gated by
  its own cooldown/chance), capped at 2 casts/pass — no more "one skill then idle".
- `HunterTrait` no longer fights the navigator (velocity drive removed; it just forces target + speed).
  Bats' redundant `orbit` skill removed (pursuit handles flight engagement).

### Removed / fixed
- Confirmed equipment applies (1.2.0 `randomizeData=false` fix); fodder kept armored + skilled.

### QOL
- `/am debug` on a looked-at mob now shows its **target, distance, and path state**.

## [1.2.0] — fixes & polish

### Fixed
- **Spawn positioning** — mobs no longer spawn too high/low, outside/under/over the arena, or stuck
  to walls. New `SpawnFinder.findNearPlayers`: ring-spawn around the players' **centroid**, on their
  **surface level** (±Y tolerance for gentle slopes/stairs), inside the arena, at a min distance — so
  mobs always appear in view.
- **Equipment / "too vanilla"** — mobs now spawn with `randomizeData = false` and are configured in a
  pre-add consumer: no vanilla random gear, no 1-tick flash, fully deterministic custom mobs. Armor +
  gear apply reliably. Fodder (zombie/skeletal grunt) given visible armor + traits/skills so even base
  hordes feel custom.
- **No more horde cooldown** — `/am start` / `/am stop` work on demand; the start/stop cooldown gate
  was removed entirely (only world/time/biome/player gating + the concurrency cap remain).

### Added
- **`scale` skill** — grow/shrink the caster via the SCALE attribute (optional speed + auto-revert).
- **`aura` skill** — Super-Saiyan aura: sustained STRENGTH (with its swirl) + a rising, tapering
  golden particle column (configurable colour/particle).
- **`thunder` skill upgraded** — `around` mode (cosmetic bolts around the caster for fear), on-player
  strikes, and an optional light `explosion` (AoE damage + knockback, block-safe) at each strike.
- **`swift` / `frenzy` traits** — sustained movement speed (self-aura).
- New mobs: `villager_saiyan` (aura + arrow barrage), `thunder_lord`, `voltaic_brute`,
  `swarm_sprinter` (shrinks + sprints), `growth_behemoth` (grows when bloodied). (45 mobs total.)
- New horde `saiyan_invasion`; lightning/saiyan/scale mobs added to several existing hordes; base
  hordes made less vanilla. (39 hordes total.)

## [1.1.0] — the expansion

### New engines
- **Trait system** — modular behaviour affixes hooked into the mob lifecycle. ~30 ids: berserker,
  cowardly, hunter (with movement `drive`), leaper, ambusher, mimic, teleporter/phase_walker, revenge,
  vampire/parasite, thorns, exploder, pack/swarm_leader, fake_death, enrage, carrier, plus synergy
  auras (buffer/commander/protector/guardian/ritualist/healer/regenerator/saboteur/disruptor/
  controller/hexer/frost_aura/siege). Extensible via `TraitRegistry`. See `docs/TRAITS.md`.
- **Variant / mutation system** — spawn-time overlays (stats/traits/skills/visuals/drops) rolled by
  weight + biome/time/weather/world/Y/player conditions. See `docs/ADVANCED.md`.
- **Rider system** — `mount:` (a mob riding another) and `riders:` (vertical stacks/columns) with
  configurable cascading death chains (KEEP/DROP/KILL/SCATTER/ENRAGE) and rider bonuses.
- **Mob-as-projectile** — `launch_mob` skill hurls live mobs with arc/homing + impact callbacks
  (damage, knockback, impact-summon, consume-or-land).
- **Director** — opt-in adaptive horde intensity that scales spawns to player performance, bounded.
- **Config versioning** — `config-version` with a forward-compat warning.

### New skills & behaviour
- `strike` (controller melee — makes goal-less mobs fight), `orbit` (aerial swarming).
- Right-click interaction with custom mobs blocked (no villager trading); aggressive villagers /
  chickens / cows / bats now viable via `hunter`(`drive`) + `strike`.

### New commands
- `variant`, `mount`, `stack`, `freeze`, `unfreeze`; `list traits`; `debug` shows traits/mount/
  passengers + the director multiplier.

### New content
- 16 new example mobs: aggressive villagers (`villager_zealot`, `illusion_villager`), 4 bat roles,
  the Chicken Apocalypse (`apocalypse_chicken` with an 8-branch variant tree, `kamikaze_chicken`,
  `chicken_king` boss), mount/stack/projectile showcases (`bonesteed`, `boneback_lancer`,
  `living_totem` + `totem_core`/`totem_crown`, `catapult_fiend`), `dire_cow`. (40 mobs total.)
- 8 new hordes: Farm Apocalypse, Bat Eclipse, Villager Revolt, Chicken Extinction, Sky Menace,
  False Peace, Cave Panic, Iron Cavalry. (38 hordes total.)

### Tests
- JUnit suite now 24 tests; no-dep verifier 493 checks (incl. trait/variant validation). All green;
  verified to enable on Paper 1.21.11.

## [1.0.0] — initial release

### Core
- Plugin bootstrap, lifecycle, atomic hot-reload, first-run example extraction.
- YAML config pipeline decoupled from Bukkit (`ConfigSource` → `ConfigSection`) for testability.
- Validation model (`ValidationReport` / `Issue` / `Checks`) with ERROR / WARN / INFO levels and
  `/am validate` dry-run reporting.

### Custom mobs
- Full `MobDefinition` engine: identity, `StatBlock` (incl. elemental damage multipliers + immunities),
  `Equipment`, `AiProfile`, data-driven skills, HP `Phase`s, custom `DropTable`, `Presentation`,
  per-player/difficulty `ScalingRule`.
- Runtime: `MobSpawner` (attribute/gear/PDC application), `ActiveMob` controller (throttled AI assist,
  target selection, leash/retreat/day-burn, regen, phases, boss bar), `MobManager` (tracking, single
  tick loop, death/drops, chunk rebinding, caps).

### Skill engine
- Extensible `Skill`/`SkillRegistry` with 34 registered ids across offense, control, movement,
  defense, summon and utility — including projectiles, thunder, dash, blink, jump-slam, area slam,
  repel/pull, summon, heal, shield, rage, fear, vanish, flight, web traps, and every status effect.
- Data-driven trigger system: `TICK / ON_DAMAGED / ON_ATTACK / ON_SPAWN / ON_DEATH / ON_LOW_HEALTH`,
  with cooldown, warmup (telegraph), chance, range, radius, duration, target rule and phase gating.

### Hordes
- `HordeDefinition` composition engine: difficulty, area (sphere/cylinder/cube, dynamic or fixed),
  spawn rules, world/biome/time/player gating, ordered waves with per-line counts/chance/roles,
  rewards (items/XP/commands), presentation.
- Runtime `HordeInstance` state machine (delay → spawn → fight → rewards), `HordeManager`
  (cooldowns, concurrency cap, gating, one tick loop), boss bar, leftover cleanup.

### Commands & API
- Root `/amazingmobs` (`/am`) with 18 subcommands, granular permissions and full tab completion,
  including command-mode mob authoring (`create`/`set`/`save`/`edit`/`clone`).
- Public `AmazingMobsApi` + 7 Bukkit events (`HordeStart` cancellable, `HordeStop`, `WaveComplete`,
  `CustomMobSpawn`, `CustomMobDeath`, `SkillTrigger`, `BossPhaseChange`).

### Content
- 24 example mobs (20 distinct archetypes + 4 fodder bodies) and 30 example hordes covering
  early→endgame, swarm, ranged, magic, aerial, siege, frozen, infernal, thunder, ritual, corrupted,
  command-led, nightmare, escalation, endurance/infinite, mini and mega events.

### Tooling
- Maven build (`pom.xml`) + dependency-free in-environment `build.sh` / `run-tests.sh`.
- JUnit 5 suite (18 tests) + a no-dependency verifier (359 checks: pure logic + every example file).
- Verified to enable on Paper 1.21.11 (24/24 mobs, 30/30 hordes, 34 skills, no errors).
