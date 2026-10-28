/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.block.Block
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedOutEvent
 *  net.neoforged.neoforge.event.tick.PlayerTickEvent$Post
 */
package dev.rustic.dungeonworld;

import dev.rustic.dungeonworld.DungeonWorld;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class ExtractionZones {
    static final Map<UUID, Progress> WAITING = new HashMap<UUID, Progress>();

    static BlockPos zone(ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        for (int dx = -3; dx <= 3; ++dx) {
            for (int dz = -3; dz <= 3; ++dz) {
                BlockPos pos = feet.offset(dx, -1, dz);
                if (!player.level().getBlockState(pos).is((Block)DungeonWorld.EXTRACTION.get())) continue;
                return pos;
            }
        }
        return null;
    }

    static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player2 = (ServerPlayer)player;
        ExtractionZones.update(player2, System.nanoTime());
    }

    static void update(ServerPlayer player, long now) {
        long remaining;
        BlockPos marker;
        UUID id = player.getUUID();
        BlockPos blockPos = marker = player.isAlive() && player.level().dimension().equals(DungeonWorld.DIMENSION) && DungeonWorld.data(player).getLong("endsAt") > System.currentTimeMillis() ? ExtractionZones.zone(player) : null;
        if (marker == null) {
            if (WAITING.remove(id) != null) {
                player.displayClientMessage((Component)Component.literal((String)"Extraction interrupted. Stay inside the zone for 60 seconds."), true);
            }
            return;
        }
        Progress progress = WAITING.get(id);
        if (progress == null || !progress.marker().equals((Object)marker)) {
            progress = new Progress(marker, now);
            WAITING.put(id, progress);
        }
        if ((remaining = Math.max(0L, 60L - (now - progress.started()) / 1000000000L)) == 0L) {
            WAITING.remove(id);
            DungeonWorld.finish(player, "Extraction complete. Returned to your entrance portal.", System.currentTimeMillis());
        } else if (player.tickCount % 20 == 0) {
            player.displayClientMessage((Component)Component.literal((String)("Extracting: " + remaining + "s \u2014 stay in the marked area")), true);
        }
    }

    static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        WAITING.remove(event.getEntity().getUUID());
    }

    record Progress(BlockPos marker, long started) {
    }
}

