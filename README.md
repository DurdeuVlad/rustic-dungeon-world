# Dungeon World

Timed adventure expeditions through a shared dungeon dimension (Minecraft 1.21.1, NeoForge 21.1.248).

Formerly "Rustic Dungeon World" (renamed in 1.1.0). Rebuilt into a maintained source repository from the compiled `rustic-dungeon-world-1.0.0.jar` (provenance: decompiled with CFR 0.152 and refactored; behavior preserved).

## Features
- One-hour expeditions through operator-built portals; four-hour cooldown on completion, expiry, leave, or death.
- Shared dungeon dimension with overworld-like terrain (dedicated biomes), WDA/Bosses'Rise/Born in Chaos structures, extraction zones, relic loot.
- Adventure-mode enforcement for non-operators; block/explosion protection inside the dimension.
- Weekly world reset (Europe/Bucharest, Monday 05:00 default) with wall-clock persistence across restarts.
- Countdown HUD via custom payload (`dungeonworld:timer`).

## Commands
| Command | Who | What |
|---|---|---|
| `/dungeonworld` | anyone | own status (remaining run / next run / ready) |
| `/dungeonworld leave` | anyone | end your expedition early, return to portal |
| `/dungeonworld portal` | op 2 | build a 4x5 crying-obsidian portal 4 blocks north |
| `/dungeonworld resetcooldown [player]` | op 2 | clear a cooldown (self by default) |
| `/dungeonworld end <player>` | op 2 | force-end a running expedition |
| `/dungeonworld list` | op 2 | active expeditions + cooldowns |
| `/dungeonworld repair` | op 2 | rebuild the missing landing platform (crash-safe) |
| `/dungeonworld reset [now]` | op 2 | show reset schedule / trigger a manual reset |
| `/dungeonworld nearest [radius]` | op 2 | bounded async extraction-zone search (default 32, max 100 chunks) |

## Notable fixes vs the original jar
- `spawn()` no longer throws `IllegalStateException` when the landing lodestone is missing — entry is refused with an actionable message and `/dungeonworld repair` rebuilds the platform.
- Missing portal apron or return spot → clear message / safe fallback instead of silent failure.
- `DungeonSpawnLimits` and the rest of the god-class internals were decompiled-and-preserved; `DungeonWorldCommands` owns the command tree.
- `WeeklyReset` keeps its path-escape guards and DST-aware scheduling; `/dungeonworld reset now` triggers it manually with the same player-return + restart-request flow. `requestNow()` stamps `completedAt=0` before halt so the next startup actually archives the dimension (1.0.0 restart-only bug fixed).
- `/dungeonworld nearest` replaces vanilla `/locate` for extraction zones: the search runs on the worldgen pool with a capped radius and replies on the server thread � vanilla locate's unbounded synchronous scan stalled a fresh dimension for minutes. Prefer it on live servers.

## Dependencies
When Dungeons Arise, Bosses'Rise (`block_factorys_bosses`), Born in Chaos, In Control — all `side=both`/`required` in `neoforge.mods.toml`.

## Build
`./gradlew build` → `build/libs/dungeonworld-<version>.jar` (JDK 21).

## Server deployment
See `DEPLOYMENT.md` in the original update package: merge `config/dungeonworld-*.toml`, the `config/incontrol/` spawn rules, and `corpse.access.only_owner=false`; run a restart supervisor (`server-restart-loop.ps1`) before enabling the weekly reset.
