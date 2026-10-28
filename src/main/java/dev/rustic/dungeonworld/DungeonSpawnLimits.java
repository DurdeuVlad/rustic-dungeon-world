/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent
 */
package dev.rustic.dungeonworld;

import dev.rustic.dungeonworld.DungeonWorld;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

public final class DungeonSpawnLimits {
    public static final String TAG = "rustic_dungeon_free_spawn";
    public static final int GLOBAL_LIMIT = 120;
    public static final int LOCAL_LIMIT = 40;

    static void finalizeSpawn(FinalizeSpawnEvent event) {
        ServerLevel level;
        Level level2;
        Mob mob = event.getEntity();
        if (!(mob.getTags().contains(TAG) && (level2 = mob.level()) instanceof ServerLevel && (level = (ServerLevel)level2).dimension().equals(DungeonWorld.DIMENSION))) {
            return;
        }
        int global = 0;
        int local = 0;
        for (Entity existing : level.getAllEntities()) {
            if (!existing.isAlive() || !existing.getTags().contains(TAG)) continue;
            ++global;
            if (existing.distanceToSqr((Entity)mob) <= 9216.0) {
                ++local;
            }
            if (global < 120 && local < 40) continue;
            event.setSpawnCancelled(true);
            return;
        }
    }
}

