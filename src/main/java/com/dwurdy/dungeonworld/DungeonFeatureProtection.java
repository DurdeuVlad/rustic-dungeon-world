/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Registry
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.WorldGenRegion
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.StructureManager
 *  net.minecraft.world.level.WorldGenLevel
 *  net.minecraft.world.level.levelgen.structure.BoundingBox
 *  net.minecraft.world.level.levelgen.structure.StructureStart
 */
package com.dwurdy.dungeonworld;

import com.dwurdy.dungeonworld.DungeonWorld;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public final class DungeonFeatureProtection {
    public static boolean terrainFeature(ResourceLocation id) {
        return id != null && id.getNamespace().equals("minecraft") && Set.of("delta", "small_basalt_columns", "large_basalt_columns", "basalt_blobs", "blackstone_blobs", "crimson_fungi", "warped_fungi", "spring_delta", "spring_closed_double").contains(id.getPath());
    }

    public static boolean overlaps(BoundingBox box, BlockPos origin) {
        return box.maxX() >= origin.getX() - 48 && box.minX() <= origin.getX() + 48 && box.maxZ() >= origin.getZ() - 48 && box.minZ() <= origin.getZ() + 48;
    }

    public static boolean blocked(WorldGenLevel world, BlockPos origin, ResourceLocation feature) {
        if (!world.getLevel().dimension().equals(DungeonWorld.DIMENSION) || !DungeonFeatureProtection.terrainFeature(feature)) {
            return false;
        }
        StructureManager manager = world.getLevel().structureManager();
        if (world instanceof WorldGenRegion) {
            WorldGenRegion region = (WorldGenRegion)world;
            manager = manager.forWorldGenRegion(region);
        }
        Registry registry = world.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (StructureStart start : manager.startsForStructure(new ChunkPos(origin), structure -> {
            ResourceLocation id = registry.getKey(structure);
            return id != null && id.getNamespace().equals("dungeonworld");
        })) {
            if (!start.isValid() || !DungeonFeatureProtection.overlaps(start.getBoundingBox(), origin)) continue;
            return true;
        }
        return false;
    }
}

