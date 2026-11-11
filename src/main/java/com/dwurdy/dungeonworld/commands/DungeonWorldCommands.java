package com.dwurdy.dungeonworld.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.datafixers.util.Pair;
import com.dwurdy.dungeonworld.DungeonWorld;
import com.dwurdy.dungeonworld.WeeklyReset;
import net.minecraft.Util;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class DungeonWorldCommands {

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dungeonworld")
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                CompoundTag d = DungeonWorld.data(p);
                long now = System.currentTimeMillis();
                if (d.getLong("endsAt") > now) {
                    DungeonWorld.message(p, "Dungeon time remaining: " + DungeonWorld.clock((d.getLong("endsAt") - now + 999L) / 1000L));
                } else if (d.getLong("cooldownUntil") > now) {
                    DungeonWorld.message(p, "Next dungeon run in " + DungeonWorld.clock((d.getLong("cooldownUntil") - now + 999L) / 1000L));
                } else {
                    DungeonWorld.message(p, "Dungeon portal ready.");
                }
                return 1;
            })
            .then(Commands.literal("leave")
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (DungeonWorld.data(p).getLong("endsAt") == 0L) {
                        DungeonWorld.message(p, "You are not on a dungeon expedition.");
                        return 0;
                    }
                    DungeonWorld.finish(p, "Dungeon expedition ended. You have returned to the overworld.", Math.min(System.currentTimeMillis(), DungeonWorld.data(p).getLong("endsAt")));
                    return 1;
                }))
            .then(Commands.literal("portal").requires(s -> s.hasPermission(2))
                .executes(c -> DungeonWorld.createPortal(c.getSource().getPlayerOrException())))
            .then(Commands.literal("resetcooldown").requires(s -> s.hasPermission(2))
                .executes(c -> resetCooldown(c.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(c -> resetCooldown(EntityArgument.getPlayer(c, "player")))))
            .then(Commands.literal("end").requires(s -> s.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(c -> {
                        ServerPlayer target = EntityArgument.getPlayer(c, "player");
                        CompoundTag d = DungeonWorld.data(target);
                        if (d.getLong("endsAt") == 0L) {
                            DungeonWorld.message(c.getSource().getPlayerOrException(), target.getGameProfile().getName() + " is not on a dungeon expedition.");
                            return 0;
                        }
                        DungeonWorld.finish(target, "Your dungeon expedition was ended by an operator.", Math.min(System.currentTimeMillis(), d.getLong("endsAt")));
                        return 1;
                    })))
            .then(Commands.literal("list").requires(s -> s.hasPermission(2))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    long now = System.currentTimeMillis();
                    int shown = 0;
                    for (ServerPlayer other : p.server.getPlayerList().getPlayers()) {
                        CompoundTag d = DungeonWorld.data(other);
                        if (d.getLong("endsAt") > now) {
                            DungeonWorld.message(p, "§e" + other.getGameProfile().getName() + "§r — running, " + DungeonWorld.clock((d.getLong("endsAt") - now + 999L) / 1000L) + " left");
                            shown++;
                        } else if (d.getLong("cooldownUntil") > now) {
                            DungeonWorld.message(p, "§7" + other.getGameProfile().getName() + " — cooldown, " + DungeonWorld.clock((d.getLong("cooldownUntil") - now + 999L) / 1000L) + " left");
                            shown++;
                        }
                    }
                    if (shown == 0) {
                        DungeonWorld.message(p, "No active expeditions or cooldowns.");
                    }
                    return shown;
                }))
            .then(Commands.literal("repair").requires(s -> s.hasPermission(2))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerLevel dim = p.server.getLevel(DungeonWorld.DIMENSION);
                    if (dim == null) {
                        DungeonWorld.message(p, "Dungeon dimension unavailable.");
                        return 0;
                    }
                    dim.setBlock(new BlockPos(0, 300, 0), Blocks.AIR.defaultBlockState(), 3);
                    BlockPos pos = DungeonWorld.spawn(dim);
                    if (pos == null) {
                        DungeonWorld.message(p, "Repair failed: no lodestone found after rebuild.");
                        return 0;
                    }
                    DungeonWorld.message(p, "Dungeon landing platform rebuilt at " + pos.toShortString() + ".");
                    return 1;
                }))
            .then(Commands.literal("nearest").requires(s -> s.hasPermission(2))
                .executes(c -> nearestExtractionZone(c.getSource().getPlayerOrException(), 32))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                    .executes(c -> nearestExtractionZone(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "radius")))))
            .then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    DungeonWorld.message(p, "Next weekly reset: " + WeeklyReset.describeNext()
                        + " (generation " + WeeklyReset.generation() + ", pending=" + WeeklyReset.pending() + ")");
                    return 1;
                })
                .then(Commands.literal("now")
                    .executes(c -> {
                        ServerPlayer p = c.getSource().getPlayerOrException();
                        if (!WeeklyReset.requestNow(p.server)) {
                            DungeonWorld.message(p, "Manual reset failed (disabled, pending, or restart-request write failed). Check the log.");
                            return 0;
                        }
                        DungeonWorld.message(p, "Manual dungeon reset scheduled — players are being returned and the server will restart.");
                        return 1;
                    })))
        );
    }

    // Bounded async locate: vanilla /locate runs an unbounded synchronous scan on the
    // server thread and stalls a fresh dimension. This resolves holders on-thread, runs
    // findNearestMapStructure on the worldgen pool with a capped radius, and replies on
    // the server thread. Structure-check chunk gen happens on worldgen workers, not the tick.
    private static int nearestExtractionZone(ServerPlayer p, int radiusChunks) {
        ServerLevel dim = p.server.getLevel(DungeonWorld.DIMENSION);
        if (dim == null) {
            DungeonWorld.message(p, "Dungeon dimension unavailable.");
            return 0;
        }
        Registry<Structure> registry = dim.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Holder<Structure> holder = registry.getHolder(ResourceKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("dungeonworld", "extraction_zone"))).orElse(null);
        if (holder == null) {
            DungeonWorld.message(p, "Extraction zone structure is not registered.");
            return 0;
        }
        BlockPos origin = p.blockPosition();
        int centerX = origin.getX() >> 4;
        int centerZ = origin.getZ() >> 4;
        DungeonWorld.message(p, "Searching for extraction zone within " + (radiusChunks * 16) + " blocks (async)...");
        Util.backgroundExecutor().execute(() -> {
            Pair<BlockPos, Holder<Structure>> result;
            try {
                result = dim.getChunkSource().getGenerator()
                    .findNearestMapStructure(dim, HolderSet.direct(holder), origin, radiusChunks, true);
            } catch (Throwable t) {
                result = null;
            }
            Pair<BlockPos, Holder<Structure>> found = result;
            p.server.execute(() -> {
                if (found == null) {
                    DungeonWorld.message(p, "No extraction zone within " + (radiusChunks * 16) + " blocks of "
                        + centerX + "," + centerZ + " (search capped; try a larger radius).");
                    return;
                }
                BlockPos pos = found.getFirst();
                int dist = (int)Math.sqrt(origin.distSqr(pos));
                Component coords = Component.literal("[" + pos.getX() + " " + pos.getY() + " " + pos.getZ() + "]")
                    .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/execute in dungeonworld:dungeon run tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ())));
                p.sendSystemMessage(Component.literal("Nearest extraction zone ").append(coords)
                    .append(Component.literal(" — " + dist + " blocks away.")));
            });
        });
        return 1;
    }

    private static int resetCooldown(ServerPlayer target) {
        DungeonWorld.data(target).remove("cooldownUntil");
        DungeonWorld.message(target, "Your dungeon cooldown has been reset.");
        return 1;
    }
}
