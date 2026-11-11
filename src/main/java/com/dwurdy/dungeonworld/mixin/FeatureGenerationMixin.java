/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.WorldGenLevel
 *  net.minecraft.world.level.chunk.ChunkGenerator
 *  net.minecraft.world.level.levelgen.placement.PlacedFeature
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.dwurdy.dungeonworld.mixin;

import com.dwurdy.dungeonworld.DungeonFeatureProtection;
import com.dwurdy.dungeonworld.DungeonWorld;
import com.dwurdy.dungeonworld.StructurePolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={PlacedFeature.class})
public abstract class FeatureGenerationMixin {
    @Inject(method={"place", "placeWithBiomeCheck"}, at={@At(value="HEAD")}, cancellable=true)
    private void filterDungeonFeatures(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ResourceLocation featureId = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).getKey((PlacedFeature)(Object)this);
        if (DungeonFeatureProtection.blocked(level, pos, featureId)) {
            cir.setReturnValue(false);
            return;
        }
        if (level.getLevel().dimension().equals(DungeonWorld.DIMENSION) && StructurePolicy.blockedFeature(level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).getKey((PlacedFeature)(Object)this))) {
            cir.setReturnValue(false);
        }
    }
}

