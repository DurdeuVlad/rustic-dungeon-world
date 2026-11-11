# Changelog

## 1.1.0
- Renamed from "Rustic Dungeon World" to **Dungeon World**. Mod id `rustic_dungeon_world` → `dungeonworld`; Java package `dev.rustic.dungeonworld` → `com.dwurdy.dungeonworld`. No behavior changes.
- **Breaking:** the dimension is now `dungeonworld:dungeon` (was `rustic_dungeon_world:dungeon`), the HUD payload is `dungeonworld:timer`, and the config file is `config/dungeonworld-reset.toml` (was `rustic_dungeon_world-reset.toml`). Update datapacks, loot predicates, In Control rules and configs that reference the old id. Existing dungeon-dimension data does not carry over.
- Unchanged for compatibility: the `rustic_dungeon_free_spawn` entity tag and the `data/rustic-dungeon-reset.properties` reset state file.
- Optional integrations (the mod loads standalone): When Dungeons Arise, Born in Chaos, In Control, Bosses' Rise.
