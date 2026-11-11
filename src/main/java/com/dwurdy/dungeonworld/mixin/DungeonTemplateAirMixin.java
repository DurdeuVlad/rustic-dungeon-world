/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.ServerLevelAccessor
 *  net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate$StructureBlockInfo
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.dwurdy.dungeonworld.mixin;

import com.dwurdy.dungeonworld.DungeonWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={BlockIgnoreProcessor.class})
public abstract class DungeonTemplateAirMixin {
    @Inject(method={"processBlock"}, at={@At(value="HEAD")}, cancellable=true)
    private void clearDungeonRooms(LevelReader world, BlockPos origin, BlockPos pivot, StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo current, StructurePlaceSettings settings, CallbackInfoReturnable<StructureTemplate.StructureBlockInfo> cir) {
        ServerLevelAccessor access;
        if ((Object)this == BlockIgnoreProcessor.STRUCTURE_AND_AIR && original.state().isAir() && world instanceof ServerLevelAccessor && (access = (ServerLevelAccessor)world).getLevel().dimension().equals(DungeonWorld.DIMENSION)) {
            cir.setReturnValue(current);
        }
    }
}

