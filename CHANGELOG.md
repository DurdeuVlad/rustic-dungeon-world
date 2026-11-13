# Changelog

## 1.1.0 - 2026-11-13

First public release. Minecraft 1.21.1, NeoForge 21.1.248+, Java 21. MIT licensed.

### Breaking
- Renamed from "Rustic Dungeon World" to **Dungeon World**. Mod id `rustic_dungeon_world` -> `dungeonworld`; Java package `dev.rustic.dungeonworld` -> `com.dwurdy.dungeonworld`.
- The dimension is now `dungeonworld:dungeon` (was `rustic_dungeon_world:dungeon`), the HUD payload is `dungeonworld:timer`, and the config file is `config/dungeonworld-reset.toml` (was `rustic_dungeon_world-reset.toml`). Update datapacks, loot predicates, In Control rules and configs that reference the old id.
- Existing dungeon-dimension data does not carry over.
- **When Dungeons Arise is now a required dependency.** Bosses' Rise, Born in Chaos and In Control remain optional.

### Added
- `/dungeonworld nearest [radius]` (op 2): bounded, off-tick search for extraction zones (default 32, max 100 chunks). Use it instead of vanilla `/locate`, which stalls a fresh dimension for minutes.
- Admin commands: `list`, `end <player>`, `resetcooldown [player]`, `repair`, `reset [now]`.

### Fixed
- Manual reset (`/dungeonworld reset now`) now archives the dimension on the next startup instead of restarting into the stale world.
- Entering with a missing landing lodestone no longer throws `IllegalStateException`; entry is refused with a clear message and `/dungeonworld repair` rebuilds the platform.
- A missing portal apron or return spot now gives a clear message or a safe fallback instead of failing silently.

### Compatibility
- Kept unchanged: the `rustic_dungeon_free_spawn` entity tag and the `data/rustic-dungeon-reset.properties` reset state file.

## 1.0.0
- Original private release (as "Rustic Dungeon World").
