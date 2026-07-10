# Permissions

| Node | Default | Grants |
|---|---|---|
| `amazingmobs.use` | everyone | base command: `help`, `list`, `info` |
| `amazingmobs.admin` | op | **everything** (parent of all nodes below) |
| `amazingmobs.reload` | op | `/am reload` |
| `amazingmobs.validate` | op | `/am validate` |
| `amazingmobs.mob.create` | op | `/am create`, `/am clone` |
| `amazingmobs.mob.edit` | op | `/am set`, `/am edit` |
| `amazingmobs.mob.save` | op | `/am save` |
| `amazingmobs.mob.delete` | op | `/am delete` |
| `amazingmobs.mob.export` | op | `/am export` |
| `amazingmobs.mob.spawn` | op | `/am spawn` |
| `amazingmobs.mob.test` | op | `/am test` |
| `amazingmobs.mob.give` | op | `/am give` |
| `amazingmobs.horde.start` | op | `/am start` |
| `amazingmobs.horde.stop` | op | `/am stop` |
| `amazingmobs.horde.status` | op | `/am status` |
| `amazingmobs.debug` | op | `/am debug` |

## Suggested setups

**Builder / event team** (can author + run events, no destructive ops):
```
amazingmobs.mob.create
amazingmobs.mob.edit
amazingmobs.mob.save
amazingmobs.mob.spawn
amazingmobs.mob.test
amazingmobs.horde.start
amazingmobs.horde.stop
amazingmobs.horde.status
```

**Full admin:**
```
amazingmobs.admin
```

Permissions are declared in `plugin.yml`; wire them to your permission plugin (LuckPerms, etc.) as
usual. `amazingmobs.use` defaults to `true` so anyone may browse the library with `/am list`/`info`.
