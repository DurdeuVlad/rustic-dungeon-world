/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.GameRules
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.event.tick.LevelTickEvent$Post
 */
package com.dwurdy.dungeonworld;

import com.dwurdy.dungeonworld.DungeonWorld;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class DungeonDayCycle {
    static final long DAY_TICKS = 6000L;
    static final long NIGHT_TICKS = 36000L;

    public static long skyTime(long gameTime) {
        long cycle = 42000L;
        long phase = Math.floorMod(gameTime, cycle);
        long sky = phase < 6000L ? phase * 12000L / 6000L : 12000L + (phase - 6000L) * 12000L / 36000L;
        return Math.floorDiv(gameTime, cycle) * 24000L + sky;
    }

    static void tick(LevelTickEvent.Post event) {
        ServerLevel level;
        Level level2 = event.getLevel();
        if (level2 instanceof ServerLevel && (level = (ServerLevel)level2).dimension().equals(DungeonWorld.DIMENSION) && level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            level.setDayTime(DungeonDayCycle.skyTime(level.getGameTime()));
        }
    }
}

