/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Holder
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.LevelHeightAccessor
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.biome.BiomeSource
 *  net.minecraft.world.level.chunk.ChunkGenerator
 *  net.minecraft.world.level.levelgen.RandomState
 *  net.minecraft.world.level.levelgen.structure.Structure
 *  net.minecraft.world.level.levelgen.structure.StructureStart
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.dwurdy.dungeonworld.mixin;

import com.dwurdy.dungeonworld.StructurePolicy;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Structure.class})
public abstract class StructureGenerationMixin {
    @Inject(method={"generate"}, at={@At(value="HEAD")}, cancellable=true)
    private void filterDungeonStructures(RegistryAccess registries, ChunkGenerator generator, BiomeSource biomes, RandomState random, StructureTemplateManager templates, long seed, ChunkPos chunk, int references, LevelHeightAccessor height, Predicate<Holder<Biome>> validBiome, CallbackInfoReturnable<StructureStart> cir) {
        if (StructurePolicy.dungeonSource(biomes) && StructurePolicy.blockedStructure(registries.registryOrThrow(Registries.STRUCTURE).getKey((Structure)(Object)this))) {
            cir.setReturnValue(StructureStart.INVALID_START);
        }
    }
}

