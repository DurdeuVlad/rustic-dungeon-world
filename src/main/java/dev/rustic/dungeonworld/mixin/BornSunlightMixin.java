/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.Entity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package dev.rustic.dungeonworld.mixin;

import dev.rustic.dungeonworld.DungeonWorld;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets={"net.mcreator.borninchaosv.procedures.DecayingZombiePriObnovlieniiTaktaSushchnostiProcedure", "net.mcreator.borninchaosv.procedures.DecrepitSkeletonPriObnovlieniiTaktaSushchnostiProcedure", "net.mcreator.borninchaosv.procedures.RestlessSpiritPriObnovlieniiTaktaSushchnostiProcedure", "net.mcreator.borninchaosv.procedures.SiameseskeletonsticProcedure"}, remap=false)
public abstract class BornSunlightMixin {
    @Redirect(method={"execute"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"), remap=false)
    private static void preventDungeonSunFire(Entity entity, float seconds) {
        if (!entity.level().dimension().equals(DungeonWorld.DIMENSION)) {
            entity.igniteForSeconds(seconds);
        }
    }
}

