/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.rustic.dungeonworld.mixin;

import dev.rustic.dungeonworld.DungeonSkyAccess;
import dev.rustic.dungeonworld.DungeonWorld;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ServerLevel.class})
public abstract class DungeonSkyWriteMixin {
    @Inject(method={"setDayTime"}, at={@At(value="HEAD")}, cancellable=true)
    private void writeDungeonSky(long time, CallbackInfo ci) {
        if (((ServerLevel)(Object)this).dimension().equals(DungeonWorld.DIMENSION)) {
            ((DungeonSkyAccess)((Object)this)).rusticSetSky(time);
            ci.cancel();
        }
    }
}

