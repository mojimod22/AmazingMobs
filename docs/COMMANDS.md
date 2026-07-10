# Commands

Root: `/amazingmobs` — aliases `/am`, `/amobs`. Tab completion is provided for every argument
(subcommands are filtered by the permissions you hold). `/am` or `/am help` lists what you can run.

| Command | Permission | Description |
|---|---|---|
| `/am help` | `amazingmobs.use` | List available commands |
| `/am list <mobs\|hordes\|skills\|traits>` | `amazingmobs.use` | List loaded definitions / registered skills / traits |
| `/am info <mob\|horde> <id>` | `amazingmobs.use` | Show details of a mob or horde |
| `/am reload` | `amazingmobs.reload` | Hot-reload config + all definitions |
| `/am validate` | `amazingmobs.validate` | Dry-run validate every file; prints per-file issues |
| `/am spawn <mobId> [amount]` | `amazingmobs.mob.spawn` | Spawn a mob where you look (amount 1–50) |
| `/am test <mobId>` | `amazingmobs.mob.test` | Spawn one and print its resolved stats/uuid |
| `/am variant <mobId> <variantId>` | `amazingmobs.mob.spawn` | *(1.1.0)* Force-spawn a specific variant (test mutations) |
| `/am mount <riderId> <mountId>` | `amazingmobs.mob.spawn` | *(1.1.0)* Spawn a mob riding another mob |
| `/am stack <baseId> <mob2> [mob3...]` | `amazingmobs.mob.spawn` | *(1.1.0)* Build a mob column (stack) |
| `/am freeze <instanceId\|all>` | `amazingmobs.horde.stop` | *(1.1.0)* Pause a running horde |
| `/am unfreeze <instanceId\|all>` | `amazingmobs.horde.stop` | *(1.1.0)* Resume a paused horde |
| `/am give <mobId> <slot\|all>` | `amazingmobs.mob.give` | Receive a mob's configured gear |
| `/am create <id> <entityType>` | `amazingmobs.mob.create` | Start a command-built mob session |
| `/am set <path> <value…>` | `amazingmobs.mob.edit` | Set a field on the mob you're editing |
| `/am addskill <id> [k=v…]` | `amazingmobs.mob.edit` | *(1.4)* Append a skill to the edit session |
| `/am addtrait <id> [k=v…]` | `amazingmobs.mob.edit` | *(1.4)* Append a trait to the edit session |
| `/am adddrop <mat> [k=v…]` | `amazingmobs.mob.edit` | *(1.4)* Append a drop to the edit session |
| `/am rmentry <listPath> <i>` | `amazingmobs.mob.edit` | *(1.4)* Remove a list entry by index |
| `/am show` | `amazingmobs.mob.edit` | *(1.4)* YAML preview of the edit session |
| `/am give-trait <id> [k=v…]` | `amazingmobs.mob.edit` | *(1.4)* Inject a trait onto the mob you look at |
| `/am give-skill <id> [k=v…]` | `amazingmobs.mob.edit` | *(1.4)* Inject a skill onto the mob you look at |
| `/am save` | `amazingmobs.mob.save` | Write the session to `mobs/<id>.yml` and load it |
| `/am edit <mobId>` | `amazingmobs.mob.edit` | Load an existing mob file into a session |
| `/am clone <sourceId> <newId>` | `amazingmobs.mob.create` | Copy an existing mob into a new session |
| `/am delete <mob\|horde> <id>` | `amazingmobs.mob.delete` | Delete a definition file + unregister it |
| `/am export <mobId>` | `amazingmobs.mob.export` | Copy a mob's YAML to `exports/` (portable) |
| `/am start <hordeId> [force]` | `amazingmobs.horde.start` | Start a horde at your location (`force` bypasses gating) |
| `/am stop <instanceId\|all>` | `amazingmobs.horde.stop` | Stop a running horde |
| `/am status` | `amazingmobs.horde.status` | Active hordes + runtime counts |
| `/am debug` | `amazingmobs.debug` | Diagnostics; inspects the mob you look at |
| `/am setarenapos` | `amazingmobs.horde.start` | *(1.5.0)* Set the global arena spawn to your position |
| `/am tpall` | `amazingmobs.horde.start` | *(1.5.0)* Teleport all online players into the arena (grounded) |
| `/am prestige` | `amazingmobs.use` | *(1.5.0)* Bank a Weight Prestige tier (requires max weight) |
| `/am class [id]` | `amazingmobs.use` | *(1.6.0)* Open the class picker, or select a class directly |
| `/am skills` | `amazingmobs.use` | *(1.6.0)* Open your skill menu (view cooldowns + cast) |

## Authoring a mob via commands

```
/am create frost_knight stray
/am set name <aqua>Frost Knight
/am set stats.health 180
/am set stats.damage 10
/am set stats.armor 12
/am set stats.damage-multipliers.fire 2.0
/am set equipment.main-hand.material IRON_SWORD
/am set presentation.boss-bar true
/am set presentation.boss-bar-color BLUE
/am save
```
`set` accepts dotted paths and coerces values (numbers, `true`/`false`, text, `min-max` ranges). For
list-shaped fields (skills, multiple drops) edit the generated `mobs/<id>.yml` directly, then
`/am reload`. `/am edit <id>` reloads a file into a session to continue editing.

## Running events
```
/am start blood_moon force      # start now, ignore time/biome/cooldown/player gating
/am status                      # blood_moon-1 · wave 2/4 · alive 37 · world
/am stop blood_moon-1           # stop one instance
/am stop all
```

## Arena flow (1.5.0)
```
/am setarenapos                 # stand on the arena floor; saves this spot (arena.yml)
/am tpall                       # gather every online player onto that floor (safely grounded)
/am start hollow_nest_war       # then start — players are healed + kitted automatically
```
When a horde starts, every online player is full-healed and given the standard battle kit
(netherite gear + sword + enchanted golden apples + potions + totems). Toggle with
`horde.equip-all-players-on-start` in `config.yml`.

## Weight & Prestige (1.5.0)
A universal, persistent per-player stat (kg). Kills add weight (scaled by mob difficulty); weight
grants more hearts, melee strength and a slight speed boost. A "⚖ WEIGHT" sidebar is always shown.
```
/am prestige                    # at 175 kg: +1 permanent heart & +strength, reset to 60 kg
```
Sit at the goal too long without prestiging and your weight decays back down — see the `weight:`
section of `config.yml` for all knobs. Full details in **[PROGRESSION.md](PROGRESSION.md)**.

## Classes & skills (1.6.0)
```
/am class                       # open the class picker (preview roles/skills), then click one
/am class necromancer           # or pick directly
/am skills                      # view your skills, cooldowns, prestige scaling — click to cast
```
In combat: **F** = active skill, **Shift+F** = special, **Shift+Q** = hyper, right-click the AK-47 =
base. Skills scale with your Weight prestige. Full roster + mechanics in **[CLASSES.md](CLASSES.md)**.
