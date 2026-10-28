/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.monster.Enemy
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.rustic.dungeonworld.mixin;

import dev.rustic.dungeonworld.DungeonWorld;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Mob.class})
public abstract class MobSunlightMixin {
    @Inject(method={"isSunBurnTick"}, at={@At(value="HEAD")}, cancellable=true)
    private void dungeonSunProtection(CallbackInfoReturnable<Boolean> cir) {
        Mob mob = (Mob)(Object)this;
        if (mob instanceof Enemy && mob.level().dimension().equals(DungeonWorld.DIMENSION)) {
            cir.setReturnValue(false);
        }
    }
}

