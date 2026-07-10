# Weight Progression (1.5.0)

A lightweight RPG layer on top of hordes. Every player carries a **Weight** (kg) — a *universal,
persistent* stat that grows as they fight and translates into raw power. It is stored in
`weights.yml`, survives restarts, and is shared across every horde (it is **not** reset per event).

## The loop

1. Everyone starts at **60 kg**.
2. **Killing mobs adds weight**, scaled by the mob's difficulty tier.
3. More weight = more **hearts**, more **melee strength**, and a **slight** speed boost.
4. At the goal (**175 kg**) you can **`/am prestige`**: bank a permanent tier and reset to 60 kg.
5. If you *don't* prestige and just sit at the goal, after **10 minutes** your weight **decays** back
   to **140 kg** — there's no parking at max for free.

## Bonuses

Bonuses scale smoothly from base (0%) to goal (100%) as `t = (weight − 60) / (175 − 60)`, applied as
keyed attribute modifiers (so they stack cleanly with gear and are re-applied on join):

| Stat | At base (60 kg) | At goal (175 kg) | Per prestige tier |
|------|-----------------|------------------|-------------------|
| Max health | +0 | **+4 hearts** (+8 HP) | **+1 heart** (+2 HP), permanent |
| Melee damage | +0 | **+3** | **+0.5**, permanent |
| Movement speed | +0 | **~+15%** of base walk speed | **~+3%**, permanent |

Tuned to feel strong-but-fair at the goal: noticeably tankier and harder-hitting, only a little
faster (no "tiny Sonic"). Prestige bonuses are permanent and **stack on top** of the weight-based
bonuses, so a high-prestige player climbing back toward the goal ends up stronger than before.

## Weight gained per kill

| Tier | Weight |
|------|--------|
| Common | 0.3 kg |
| Uncommon | 0.5 kg |
| Rare | 1.0 kg |
| Elite | 2.0 kg |
| Miniboss | 3.5 kg |
| Boss | **5.0 kg** |
| *(any vanilla mob)* | 0.2 kg |

So trash mobs nudge you up by hundreds of grams; bosses are worth a few kilos.

## Prestige

```
/am prestige
```
Requires being at the goal (175 kg). On success: **prestige level +1** (it starts at 1), a permanent
**+1 heart** and **+strength**, and your weight resets to **60 kg** so you can climb again. The
sidebar shows your current tier.

## Sidebar

A **"⚖ WEIGHT"** sidebar is shown to **every player at all times** (not just during hordes), with
their current weight, the goal, and their prestige tier. It updates live on every kill, prestige, and
decay. (Set `weight.enabled: false` to remove it along with the rest of the system.)

## Configuration

```yaml
weight:
  enabled: true
  base-kg: 60        # everyone starts here; prestige resets here
  goal-kg: 175       # reach this for max bonuses + the ability to prestige
  decay-to-kg: 140   # where you slip back to if you park at the goal
  decay-minutes: 10  # grace period at the goal before decay
```

Set `enabled: false` to switch the whole system off (no bonuses, no sidebar, no kill rewards, no
`/am prestige`).

## Notes & limits

- Weight is **server-wide and per-player** — there is one arena and one shared progression for now
  (this is intentionally simple; per-arena / per-world scoping is a future option).
- Decay counts **offline time too**: logging off parked at the goal still decays you, so you can't
  relog to dodge it.
- Bonuses are transient attribute modifiers re-applied on join; they never get "baked into" the
  player, so disabling the system cleanly removes them.
