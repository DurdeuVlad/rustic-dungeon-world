/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.storage.LevelResource
 *  net.neoforged.neoforge.common.ModConfigSpec
 *  net.neoforged.neoforge.common.ModConfigSpec$BooleanValue
 *  net.neoforged.neoforge.common.ModConfigSpec$Builder
 *  net.neoforged.neoforge.common.ModConfigSpec$ConfigValue
 *  net.neoforged.neoforge.common.ModConfigSpec$EnumValue
 *  net.neoforged.neoforge.common.ModConfigSpec$IntValue
 *  net.neoforged.neoforge.event.server.ServerAboutToStartEvent
 *  net.neoforged.neoforge.event.tick.ServerTickEvent$Post
 *  org.slf4j.Logger
 */
package dev.rustic.dungeonworld;

import com.mojang.logging.LogUtils;
import dev.rustic.dungeonworld.DungeonWorld;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

public final class WeeklyReset {
    private static final Logger LOG = LogUtils.getLogger();
    static final ModConfigSpec SPEC;
    static final ModConfigSpec.BooleanValue ENABLED;
    static final ModConfigSpec.ConfigValue<String> ZONE;
    static final ModConfigSpec.EnumValue<DayOfWeek> DAY;
    static final ModConfigSpec.IntValue HOUR;
    private static long completedAt;
    private static long generation;
    private static boolean pending;
    private static boolean warnedFive;
    private static boolean warnedOne;
    private static Path worldRoot;

    public static long generation() {
        return generation;
    }

    public static boolean pending() {
        return pending;
    }

    public static String describeNext() {
        try {
            Instant next = next(Instant.now(), ZoneId.of(ZONE.get()), DAY.get(), HOUR.get());
            return next.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /** Operator-triggered reset: returns players, writes the restart request, halts. */
    public static boolean requestNow(MinecraftServer server) {
        if (!enabled(server) || pending) {
            return false;
        }
        pending = true;
        Instant now = Instant.now();
        for (ServerPlayer p : List.copyOf(server.getPlayerList().getPlayers())) {
            if (DungeonWorld.data(p).getLong("endsAt") > 0L || p.level().dimension().equals(DungeonWorld.DIMENSION)) {
                DungeonWorld.finish(p, "Weekly dungeon reset: returned to your entrance portal.", Math.min(now.toEpochMilli(), DungeonWorld.data(p).getLong("endsAt") > 0L ? DungeonWorld.data(p).getLong("endsAt") : now.toEpochMilli()));
            }
            if (p.level().dimension().equals(DungeonWorld.DIMENSION)) {
                pending = false;
                LOG.error("Weekly reset delayed: failed to return player {}", p.getGameProfile().getName());
                return false;
            }
        }
        try {
            Files.writeString(server.getServerDirectory().resolve(".dungeon-world-restart-request"), "manual-dungeon-reset\n");
        } catch (IOException e) {
            pending = false;
            LOG.error("Manual reset failed: cannot write restart request", e);
            return false;
        }
        broadcast(server, "Manual dungeon reset: the server is restarting. Reconnect shortly.");
        server.halt(false);
        return true;
    }

    static Instant latest(Instant now, ZoneId zone, DayOfWeek day, int hour) {
        ZonedDateTime local = now.atZone(zone);
        LocalDate date = local.toLocalDate().with(TemporalAdjusters.previousOrSame(day));
        ZonedDateTime candidate = date.atTime(hour, 0).atZone(zone);
        if (candidate.toInstant().isAfter(now)) {
            candidate = date.minusWeeks(1L).atTime(hour, 0).atZone(zone);
        }
        return candidate.toInstant();
    }

    static Instant next(Instant now, ZoneId zone, DayOfWeek day, int hour) {
        return WeeklyReset.latest(now, zone, day, hour).atZone(zone).plusWeeks(1L).toInstant();
    }

    private static boolean enabled(MinecraftServer server) {
        return (Boolean)ENABLED.get() != false && server.isDedicatedServer();
    }

    static void beforeStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        warnedOne = false;
        warnedFive = false;
        pending = false;
        completedAt = 0L;
        generation = 0L;
        if (!WeeklyReset.enabled(server)) {
            return;
        }
        try {
            worldRoot = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            Path stateFile = worldRoot.resolve("data/rustic-dungeon-reset.properties");
            Properties state = new Properties();
            long due = WeeklyReset.latest(Instant.now(), ZoneId.of((String)ZONE.get()), (DayOfWeek)DAY.get(), (Integer)HOUR.get()).toEpochMilli();
            if (Files.exists(stateFile, new LinkOption[0])) {
                try (InputStream in = Files.newInputStream(stateFile, new OpenOption[0]);){
                    state.load(in);
                }
                completedAt = Long.parseLong(state.getProperty("completedAt"));
                generation = Long.parseLong(state.getProperty("generation", "0"));
                if (completedAt < due) {
                    Path archive = WeeklyReset.archiveDimension(worldRoot, due);
                    completedAt = due;
                    LOG.info("Dungeon weekly reset completed; generation {}, archive {}", (++generation), (Object)archive);
                }
            } else {
                completedAt = due;
            }
            state.setProperty("completedAt", Long.toString(completedAt));
            state.setProperty("generation", Long.toString(generation));
            Files.createDirectories(stateFile.getParent(), new FileAttribute[0]);
            Path temp = stateFile.resolveSibling(String.valueOf(stateFile.getFileName()) + ".tmp");
            try (OutputStream out = Files.newOutputStream(temp, new OpenOption[0]);){
                state.store(out, "Dungeon reset schedule, real UTC epoch milliseconds");
            }
            Files.move(temp, stateFile, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (Exception e) {
            throw new IllegalStateException("Cannot safely prepare the weekly dungeon reset", e);
        }
    }

    static Path archiveDimension(Path root, long due) throws IOException {
        Path target = (root = root.toAbsolutePath().normalize()).resolve("dimensions/rustic_dungeon_world/dungeon").normalize();
        if (!target.startsWith(root.resolve("dimensions"))) {
            throw new IOException("Dimension path escaped the world");
        }
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        if (Files.isSymbolicLink(target) || !target.toRealPath(new LinkOption[0]).startsWith(root.toRealPath(new LinkOption[0]).resolve("dimensions"))) {
            throw new IOException("Refusing to reset a dimension outside the world or through a symlink");
        }
        Path backups = root.resolve("dungeon-world-backups");
        Files.createDirectories(backups, new FileAttribute[0]);
        if (!backups.toRealPath(new LinkOption[0]).startsWith(root.toRealPath(new LinkOption[0]))) {
            throw new IOException("Backup path escaped the world");
        }
        Path destination = backups.resolve(due + "-" + String.valueOf(UUID.randomUUID()));
        Files.move(target, destination, new CopyOption[0]);
        return destination;
    }

    static void tick(ServerTickEvent.Post event) {
        ZoneId zone;
        MinecraftServer server = event.getServer();
        if (!WeeklyReset.enabled(server) || pending || server.getTickCount() % 20 != 0) {
            return;
        }
        Instant now = Instant.now();
        long due = WeeklyReset.latest(now, zone = ZoneId.of((String)ZONE.get()), (DayOfWeek)DAY.get(), (Integer)HOUR.get()).toEpochMilli();
        if (due > completedAt) {
            pending = true;
            for (ServerPlayer p : List.copyOf(server.getPlayerList().getPlayers())) {
                if (DungeonWorld.data(p).getLong("endsAt") > 0L || p.level().dimension().equals(DungeonWorld.DIMENSION)) {
                    DungeonWorld.finish(p, "Weekly dungeon reset: returned to your entrance portal.", Math.min(now.toEpochMilli(), DungeonWorld.data(p).getLong("endsAt") > 0L ? DungeonWorld.data(p).getLong("endsAt") : now.toEpochMilli()));
                }
                if (!p.level().dimension().equals(DungeonWorld.DIMENSION)) continue;
                pending = false;
                LOG.error("Weekly reset delayed: failed to return player {}", (Object)p.getGameProfile().getName());
                return;
            }
            try {
                Files.writeString(server.getServerDirectory().resolve(".dungeon-world-restart-request"), (CharSequence)"weekly-dungeon-reset\n", new OpenOption[0]);
            }
            catch (IOException e) {
                pending = false;
                LOG.error("Weekly reset delayed: cannot write restart request", (Throwable)e);
                return;
            }
            WeeklyReset.broadcast(server, "Weekly dungeon reset: the server is restarting. Reconnect shortly.");
            server.halt(false);
            return;
        }
        long seconds = Duration.between(now, WeeklyReset.next(now, zone, (DayOfWeek)DAY.get(), (Integer)HOUR.get())).getSeconds();
        if (seconds > 300L) {
            warnedOne = false;
            warnedFive = false;
        }
        if (seconds <= 300L && !warnedFive) {
            warnedFive = true;
            WeeklyReset.broadcast(server, "Weekly dungeon reset and server restart in 5 minutes. Dungeon terrain, loot, items and corpses will reset.");
        }
        if (seconds <= 60L && !warnedOne) {
            warnedOne = true;
            WeeklyReset.broadcast(server, "Weekly dungeon reset and server restart in 1 minute. Players will return to their entrance portals.");
        }
    }

    private static void broadcast(MinecraftServer server, String text) {
        server.getPlayerList().broadcastSystemMessage((Component)Component.literal((String)text), false);
    }

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ENABLED = b.comment("Dedicated servers only. Requires a restart wrapper or panel supervisor for automatic restart.").define("enabled", true);
        ZONE = b.define("timezone", "Europe/Bucharest", value -> {
            try {
                ZoneId.of(String.valueOf(value));
                return true;
            }
            catch (Exception e) {
                return false;
            }
        });
        DAY = b.defineEnum("day", (Enum)DayOfWeek.MONDAY);
        HOUR = b.defineInRange("hour", 5, 0, 23);
        SPEC = b.build();
    }
}

