# Player Classes & Skills (1.6.0)

A full combat-progression layer on top of hordes. Pick a class, wield an **active / special / hyper**
skill set on top of the universal **AK-47 base**, and watch every skill grow with your **Weight
prestige**. Each class plays differently — a build, a role, a fantasy — not just a reskin.

## Triggers (how you cast)

Minecraft has no custom-keybind API, so skills are bound to standard keys (only intercepted for players
who have a class — everyone else swaps/drops normally):

| Action | Key | What it casts |
|--------|-----|---------------|
| **Base** | Right-click the **AK-47** item | your reliable fallback rifle |
| **Active** | **F** (swap-hand) | frequent, low-cooldown tool |
| **Special** | **Shift + F** | impactful mid-cooldown play |
| **Hyper** | **Shift + Q** (drop) | spectacular, long-cooldown near-ultimate |
| **Skill items** | **Right-click** an item from the horde kit | casts that skill (alternative to the keys) |
| **Menu** | `/am skills` | view all skills, cooldowns, prestige scaling, and click to cast |

On horde start, classed players receive their active / special / hyper as **physical skill items** in
hotbar slots 2–4 (slot 0 = sword, 1 = AK-47) — right-click one to cast it. So you can play by keybind
**or** by item, whichever you prefer.

> Because **F** is the active-skill key, classed players don't off-hand-swap normally, and **Shift+Q**
> is the hyper key (plain **Q** still drops). This is the cost of real "keybinds" without a client mod.

## Choosing a class

```
/am class            # open the class picker GUI (preview role, passives, skills)
/am class <id>       # pick directly, e.g. /am class necromancer
```
Your choice is **persistent** (saved in `classes.yml`, survives restart/relogin). Re-picking is allowed
unless `classes.allow-changing: false`. Switching clears your old cooldowns + dismisses your minions.

> **Prestige resets your class.** Banking a Weight Prestige (`/am prestige`) wipes your class so you
> choose a fresh one — every tier is a chance to re-spec your build.

## The roster

| Class | Role | Passive | Active | Special | Hyper |
|-------|------|---------|--------|---------|-------|
| **Necromancer** | Summoner / attrition | Soul Harvest: kills may raise minions; cap grows with prestige | Soul Bolt | Raise Dead | Army of the Damned |
| **Stormcaller** | Ranged control | Conductor: bolts chain | Chain Lightning | Thunderstrike | Tempest |
| **Pyromancer** | Area damage | Cinderskin: fire-immune | Cinder Blast | Meteor | Inferno |
| **Vanguard** | Tank / frontline | Ironclad: +resistance, +4 hearts | Shield Bash | Bulwark | Unbreakable |
| **Assassin** | Mobility / burst | Fleet: permanent speed | Shadow Dash | Shadowstrike | Death Mark |
| **Guardian** | Support / sustain | Lightbearer: self-regen + team buffs | Healing Pulse | Rally | Sanctuary |

Roles are complementary: Pyromancer/Stormcaller shred swarms, Assassin/Vanguard handle elites & bosses,
Guardian sustains the squad, Necromancer turns the horde's own numbers against it.

### The Necromancer (flagship)
- **Soul Harvest (passive):** a mob you kill has a chance (35% → 75% with prestige) to **rise as a
  permanent minion** that fights for you, up to a prestige-scaled cap (`necromancer-minion-base-cap`
  + `…-per-prestige`). Stronger foes rise as wither-skeleton reavers, the rest as skeletons.
- **Soul Bolt (active):** a necrotic bolt — damage + Wither, and **heals you if it lands a kill**.
- **Raise Dead (special):** tear a pack of skeletons from the ground into your standing army.
- **Army of the Damned (hyper):** a temporary host of wither-skeleton reavers + a necrotic aura
  (enemies rot & slow; you gain Strength + Absorption).
- Minions **glow** (easy to read), chase the nearest enemy, follow you when idle, and **fall when you
  die / log out / change class**. Orphans are swept on restart (no leaks).

## Prestige scaling

Skills read your **Weight prestige** (see [PROGRESSION.md](PROGRESSION.md)). Higher prestige means:
- **−cooldown** (up to −50% at high prestige, times `classes.cooldown-multiplier`),
- **+damage / +duration**,
- **+summon count** (Raise Dead, Army of the Damned),
- **+chain jumps / +strikes** (Chain Lightning, Tempest),
- **higher minion cap** (Necromancer).

The skill menu shows the **effective** cooldown at your current prestige, so growth is legible.

## Configuration (`config.yml` → `classes:`)

```yaml
classes:
  enabled: true
  allow-changing: true                  # may players re-pick their class?
  cooldown-multiplier: 1.0              # global multiplier on every skill cooldown
  necromancer-minion-base-cap: 6        # max risen minions at prestige 1
  necromancer-minion-per-prestige: 0.5  # +cap per prestige (0.5 = +1 every 2 prestiges)
```
Set `enabled: false` to switch the whole system off (no triggers, menus, minions, or commands).

## Reliability notes
- Cooldowns are per-player, runtime-only (a fresh login starts ready).
- Passive buffs are infinite potion effects, re-applied on join/respawn and topped up every 5s; cleared
  in bulk when you change class.
- Minions are tracked + capped + swept on restart; minion kills don't raise more minions (no runaway).
- Every skill cast is wrapped — a misfiring skill logs a warning, it never crashes the tick.
