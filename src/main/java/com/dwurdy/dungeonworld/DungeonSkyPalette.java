/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.phys.Vec3
 */
package com.dwurdy.dungeonworld;

import net.minecraft.world.phys.Vec3;

public final class DungeonSkyPalette {
    private static final Vec3 DAY = Vec3.fromRGB24((int)16747556);
    private static final Vec3 NIGHT = Vec3.fromRGB24((int)3671564);

    public static Vec3 color(double dayTime) {
        double daylight = Math.max(0.0, Math.min(1.0, Math.cos((dayTime - 6000.0) * Math.PI / 12000.0) * 2.0 + 0.5));
        return NIGHT.lerp(DAY, daylight);
    }
}

