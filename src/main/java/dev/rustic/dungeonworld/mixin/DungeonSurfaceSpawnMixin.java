/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.rustic.dungeonworld.mixin;

import dev.rustic.dungeonworld.DungeonWorld;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets={"mcjty.incontrol.spawner.SpawnerSystem"}, remap=false)
public abstract class DungeonSurfaceSpawnMixin {
    @Inject(method={"getValidSpawnablePosition"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private static void chooseDungeonSurface(LevelReader world, int x, int z, int minHeight, int maxHeight, Predicate<BlockPos> valid, CallbackInfoReturnable<BlockPos> cir) {
        int top;
        ServerLevel level;
        if (!(world instanceof ServerLevel) || !(level = (ServerLevel)world).dimension().equals(DungeonWorld.DIMENSION)) {
            return;
        }
        for (int y = top = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z); y >= Math.max(minHeight, top - 8); --y) {
            BlockPos surface = new BlockPos(x, y, z);
            if (y > maxHeight || !valid.test(surface)) continue;
            cir.setReturnValue(surface);
            return;
        }
        cir.setReturnValue(null);
    }
}

