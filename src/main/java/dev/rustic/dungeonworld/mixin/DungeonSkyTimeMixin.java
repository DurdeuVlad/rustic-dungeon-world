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
package dev.rustic.dungeonworld.mixin;

import dev.rustic.dungeonworld.DungeonDayCycle;
import dev.rustic.dungeonworld.DungeonSkyAccess;
import dev.rustic.dungeonworld.DungeonWorld;
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
    private Long rusticDungeonSky;

    @Inject(method={"getDayTime"}, at={@At(value="HEAD")}, cancellable=true)
    private void readDungeonSky(CallbackInfoReturnable<Long> cir) {
        if ((Object)this instanceof ServerLevel level && level.dimension().equals(DungeonWorld.DIMENSION)) {
            if (this.rusticDungeonSky == null) {
                this.rusticDungeonSky = DungeonDayCycle.skyTime(level.getGameTime());
            }
            cir.setReturnValue(this.rusticDungeonSky);
        }
    }

    @Override
    public void rusticSetSky(long time) {
        this.rusticDungeonSky = time;
    }
}

