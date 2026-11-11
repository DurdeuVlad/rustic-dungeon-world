/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.player.Abilities
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.context.UseOnContext
 *  net.neoforged.neoforge.common.CommonHooks
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package com.dwurdy.dungeonworld.mixin;

import com.dwurdy.dungeonworld.DungeonWorld;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={CommonHooks.class}, remap=false)
public abstract class DungeonClimbingHooksMixin {
    @Redirect(method={"onPlaceItemIntoWorld"}, at=@At(value="FIELD", target="Lnet/minecraft/world/entity/player/Abilities;mayBuild:Z", remap=true), remap=false)
    private static boolean allowDungeonClimbing(Abilities abilities, UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        return abilities.mayBuild || context.getLevel().dimension().equals(DungeonWorld.DIMENSION) && (stack.is(Items.LADDER) || stack.is(Items.SCAFFOLDING));
    }
}

