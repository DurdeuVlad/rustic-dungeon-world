/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  net.minecraft.util.KeyDispatchDataCodec
 *  net.minecraft.world.level.levelgen.DensityFunction
 *  net.minecraft.world.level.levelgen.DensityFunction$FunctionContext
 *  net.minecraft.world.level.levelgen.DensityFunction$SimpleFunction
 */
package com.dwurdy.dungeonworld;

import com.mojang.serialization.MapCodec;
import com.dwurdy.dungeonworld.NaturalDungeonBiomeSource;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public final class ForestTerrainMask
implements DensityFunction.SimpleFunction {
    public static final MapCodec<ForestTerrainMask> CODEC = MapCodec.unit(new ForestTerrainMask());

    public double compute(DensityFunction.FunctionContext context) {
        return ForestTerrainMask.weight(context.blockX(), context.blockZ());
    }

    static double weight(int blockX, int blockZ) {
        double x = (double)blockX / 4.0;
        double z = (double)blockZ / 4.0;
        int cellX = (int)Math.floor(x / 96.0);
        int cellZ = (int)Math.floor(z / 96.0);
        double forest = Double.POSITIVE_INFINITY;
        double rugged = Double.POSITIVE_INFINITY;
        for (int dx = -2; dx <= 2; ++dx) {
            for (int dz = -2; dz <= 2; ++dz) {
                int cx = cellX + dx;
                int cz = cellZ + dz;
                double px = (double)cx * 96.0 + 12.0 + (double)NaturalDungeonBiomeSource.indexFor(cx * 32, cz * 32, 7200) / 100.0;
                double pz = (double)cz * 96.0 + 12.0 + (double)NaturalDungeonBiomeSource.indexFor(cz * 32 + 3456, cx * 32 - 6784, 7200) / 100.0;
                double distance = Math.hypot(x - px, z - pz);
                int biome = NaturalDungeonBiomeSource.indexFor(cx * 32, cz * 32, 4);
                if (biome == 1 || biome == 3) {
                    forest = Math.min(forest, distance);
                    continue;
                }
                rugged = Math.min(rugged, distance);
            }
        }
        double t = Math.max(0.0, Math.min(1.0, 0.5 + (rugged - forest) / 64.0));
        return t * t * (3.0 - 2.0 * t);
    }

    public double minValue() {
        return 0.0;
    }

    public double maxValue() {
        return 1.0;
    }

    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }
}

