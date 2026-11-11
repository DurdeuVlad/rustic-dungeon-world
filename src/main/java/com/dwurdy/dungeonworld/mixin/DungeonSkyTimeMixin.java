/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.dwurdy.dungeonworld.mixin;

import com.dwurdy.dungeonworld.DungeonDayCycle;
import com.dwurdy.dungeonworld.DungeonSkyAccess;
import com.dwurdy.dungeonworld.DungeonWorld;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Level.class})
public abstract class DungeonSkyTimeMixin
implements DungeonSkyAccess {
    @Unique
    private Long dungeonSky;

    @Inject(method={"getDayTime"}, at={@At(value="HEAD")}, cancellable=true)
    private void readDungeonSky(CallbackInfoReturnable<Long> cir) {
        if ((Object)this instanceof ServerLevel level && level.dimension().equals(DungeonWorld.DIMENSION)) {
            if (this.dungeonSky == null) {
                this.dungeonSky = DungeonDayCycle.skyTime(level.getGameTime());
            }
            cir.setReturnValue(this.dungeonSky);
        }
    }

    @Override
    public void setDungeonSky(long time) {
        this.dungeonSky = time;
    }
}

