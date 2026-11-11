/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.state.pattern.BlockInWorld
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.dwurdy.dungeonworld.mixin;

import com.dwurdy.dungeonworld.DungeonWorld;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ItemStack.class})
public abstract class DungeonClimbingItemMixin {
    @Inject(method={"canPlaceOnBlockInAdventureMode"}, at={@At(value="HEAD")}, cancellable=true)
    private void allowDungeonClimbing(BlockInWorld target, CallbackInfoReturnable<Boolean> cir) {
        Level level;
        LevelReader levelReader;
        ItemStack stack = (ItemStack)(Object)this;
        if ((stack.is(Items.LADDER) || stack.is(Items.SCAFFOLDING)) && (levelReader = target.getLevel()) instanceof Level && (level = (Level)levelReader).dimension().equals(DungeonWorld.DIMENSION)) {
            cir.setReturnValue(true);
        }
    }
}

