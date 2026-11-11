package com.dwurdy.dungeonworld;

import com.dwurdy.dungeonworld.commands.DungeonWorldCommands;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(value = "dungeonworld")
public final class DungeonWorld {
    public static final String ID = "dungeonworld";
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, id("dungeon"));
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    private static final DeferredRegister<com.mojang.serialization.MapCodec<? extends BiomeSource>> BIOME_SOURCES = DeferredRegister.create(Registries.BIOME_SOURCE, ID);
    private static final DeferredRegister<com.mojang.serialization.MapCodec<? extends DensityFunction>> DENSITIES;
    public static final DeferredBlock<Block> EXTRACTION;
    public static final DeferredBlock<PortalBlock> PORTAL;
    private static final String DATA = "dungeonworld";
    private static final Set<UUID> TRANSFERRING;
    private static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue RUN_SECONDS;
    public static final ModConfigSpec.IntValue COOLDOWN_SECONDS;

    public DungeonWorld(IEventBus bus, ModContainer container) {
        BLOCKS.register(bus);
        BIOME_SOURCES.register(bus);
        DENSITIES.register(bus);
        container.registerConfig(ModConfig.Type.COMMON, (IConfigSpec) SPEC);
        container.registerConfig(ModConfig.Type.COMMON, (IConfigSpec) WeeklyReset.SPEC, "dungeonworld-reset.toml");
        NeoForge.EVENT_BUS.addListener(WeeklyReset::beforeStart);
        NeoForge.EVENT_BUS.addListener(WeeklyReset::tick);
        NeoForge.EVENT_BUS.addListener(DungeonSpawnLimits::finalizeSpawn);
        NeoForge.EVENT_BUS.addListener(ExtractionZones::tick);
        NeoForge.EVENT_BUS.addListener(ExtractionZones::logout);
        NeoForge.EVENT_BUS.addListener(DungeonRelicLoot::load);
        NeoForge.EVENT_BUS.addListener(DungeonDayCycle::tick);
        bus.addListener(DungeonWorld::network);
        NeoForge.EVENT_BUS.addListener(DungeonWorldCommands::register);
        NeoForge.EVENT_BUS.addListener(DungeonWorld::tick);
        NeoForge.EVENT_BUS.addListener(DungeonWorld::clonePlayer);
        NeoForge.EVENT_BUS.addListener(DungeonWorld::respawn);
        NeoForge.EVENT_BUS.addListener(DungeonWorld::travel);
        NeoForge.EVENT_BUS.addListener(DungeonWorld::breakBlock);
        NeoForge.EVENT_BUS.addListener(DungeonWorld::placeBlock);
        NeoForge.EVENT_BUS.addListener(DungeonWorld::explosion);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    public static CompoundTag data(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(DATA)) {
            root.put(DATA, new CompoundTag());
        }
        return root.getCompound(DATA);
    }

    public static String clock(long seconds) {
        seconds = Math.max(0L, seconds);
        return String.format("%02d:%02d:%02d", seconds / 3600L, seconds / 60L % 60L, seconds % 60L);
    }

    public static void message(ServerPlayer p, String text) {
        p.sendSystemMessage(Component.literal(text));
    }

    public static boolean dungeon(Level l) {
        return l.dimension().equals(DIMENSION);
    }

    public static boolean operator(ServerPlayer p) {
        return p.hasPermissions(2);
    }

    static boolean move(ServerPlayer p, ServerLevel target, double x, double y, double z, float yaw) {
        TRANSFERRING.add(p.getUUID());
        try {
            p.teleportTo(target, x, y, z, Set.of(), yaw, 0.0f);
            p.setDeltaMovement(0.0, 0.0, 0.0);
            p.fallDistance = 0.0f;
            return p.serverLevel() == target;
        } finally {
            TRANSFERRING.remove(p.getUUID());
        }
    }

    static void enter(ServerPlayer p, BlockPos portal) {
        if (WeeklyReset.pending()) {
            return;
        }
        if (!p.isAlive() || p.isPassenger() || p.isVehicle() || !p.serverLevel().dimension().equals(Level.OVERWORLD)) {
            return;
        }
        CompoundTag d = data(p);
        long now = System.currentTimeMillis();
        if (d.getLong("portalMessageAt") > now) {
            return;
        }
        d.putLong("portalMessageAt", now + 3000L);
        if (d.getLong("endsAt") > 0L) {
            return;
        }
        long remaining = d.getLong("cooldownUntil") - now;
        if (remaining > 0L) {
            message(p, "Dungeon portal: wait " + clock((remaining + 999L) / 1000L) + " before your next run.");
            return;
        }
        ServerLevel target = p.server.getLevel(DIMENSION);
        if (target == null) {
            message(p, "Dungeon dimension is unavailable. Restart the server with this addon installed.");
            return;
        }
        BlockPos base = portal;
        while (p.level().getBlockState(base.below()).is(PORTAL.get())) {
            base = base.below();
        }
        BlockPos back = base.offset(0, 0, 2);
        if (!safe(p.serverLevel(), back)) {
            message(p, "Clear two blocks of space on the front apron of this portal.");
            return;
        }
        d.putDouble("returnX", back.getX() + 0.5);
        d.putDouble("returnY", back.getY());
        d.putDouble("returnZ", back.getZ() + 0.5);
        d.putBoolean("hasReturn", true);
        BlockPos arrival = spawn(target);
        if (arrival == null) {
            message(p, "Dungeon landing platform is missing. Ask an operator to run /dungeonworld repair.");
            return;
        }
        d.putLong("endsAt", now + RUN_SECONDS.get() * 1000L);
        d.putLong("resetGeneration", WeeklyReset.generation());
        if (!move(p, target, arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5, 0.0f)) {
            d.remove("endsAt");
            message(p, "Dungeon entry was blocked; no cooldown was charged.");
            return;
        }
        if (!operator(p)) {
            p.setGameMode(GameType.ADVENTURE);
        }
        sync(p, d.getLong("endsAt"));
        message(p, "Dungeon expedition started: " + clock(RUN_SECONDS.get()) + ". Use /dungeonworld leave to return early.");
    }

    /** Returns the landing position, or null if the platform is gone (no crash). */
    public static BlockPos spawn(ServerLevel level) {
        int y;
        BlockPos marker = new BlockPos(0, 300, 0);
        if (!level.getBlockState(marker).is(Blocks.LODESTONE)) {
            y = Math.min(290, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0) + 1);
            level.setBlock(marker, Blocks.LODESTONE.defaultBlockState(), 3);
            for (int x = -3; x <= 3; ++x) {
                for (int z = -3; z <= 3; ++z) {
                    level.setBlock(new BlockPos(x, y - 1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                    for (int dy = 0; dy < 3; ++dy) {
                        level.setBlock(new BlockPos(x, y + dy, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
            level.setBlock(new BlockPos(0, y - 1, 0), Blocks.LODESTONE.defaultBlockState(), 3);
        }
        for (y = 290; y >= -60; --y) {
            if (!level.getBlockState(new BlockPos(0, y - 1, 0)).is(Blocks.LODESTONE)) continue;
            return new BlockPos(0, y, 0);
        }
        return null;
    }

    static boolean safe(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
            && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
            && level.getFluidState(pos).isEmpty()
            && level.getFluidState(pos.above()).isEmpty()
            && level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below());
    }

    static BlockPos fallback(ServerLevel level, BlockPos origin) {
        for (int r = 0; r <= 8; ++r) {
            for (int x = -r; x <= r; ++x) {
                for (int z = -r; z <= r; ++z) {
                    for (int dy = -3; dy <= 3; ++dy) {
                        BlockPos pos = origin.offset(x, dy, z);
                        if (!safe(level, pos)) continue;
                        return pos;
                    }
                }
            }
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
        return new BlockPos(origin.getX(), y, origin.getZ());
    }

    public static void finish(ServerPlayer p, String reason, long endedAt) {
        CompoundTag d = data(p);
        boolean active = d.getLong("endsAt") > 0L;
        ServerLevel overworld = p.server.overworld();
        BlockPos wanted = d.getBoolean("hasReturn")
            ? BlockPos.containing(d.getDouble("returnX"), d.getDouble("returnY"), d.getDouble("returnZ"))
            : overworld.getSharedSpawnPos();
        BlockPos back = safe(overworld, wanted) ? wanted : fallback(overworld, wanted);
        if (!move(p, overworld, back.getX() + 0.5, back.getY(), back.getZ() + 0.5, 180.0f)) {
            return;
        }
        if (active) {
            d.putLong("cooldownUntil", endedAt + COOLDOWN_SECONDS.get() * 1000L);
        }
        d.remove("endsAt");
        d.putLong("portalMessageAt", System.currentTimeMillis() + 3000L);
        if (!operator(p)) {
            p.setGameMode(GameType.SURVIVAL);
        }
        sync(p, 0L);
        message(p, reason);
    }

    private static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer p) || p.tickCount % 20 != 0 || !p.isAlive()) {
            return;
        }
        CompoundTag d = data(p);
        long end = d.getLong("endsAt");
        if (end > 0L) {
            if (d.getLong("resetGeneration") < WeeklyReset.generation()) {
                finish(p, "The dungeon world reset while you were offline. Returned to your portal.", Math.min(System.currentTimeMillis(), end));
                return;
            }
            if (System.currentTimeMillis() >= end) {
                finish(p, "Dungeon time expired. You have returned to the overworld.", end);
                return;
            }
            if (!dungeon(p.level())) {
                finish(p, "Dungeon expedition ended after leaving the dimension.", System.currentTimeMillis());
                return;
            }
            if (!operator(p) && p.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
                p.setGameMode(GameType.ADVENTURE);
            }
            sync(p, end);
        } else if (dungeon(p.level())) {
            finish(p, "Enter the dungeon world through a dungeon portal.", System.currentTimeMillis());
        }
    }

    private static void clonePlayer(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            p.getPersistentData().put(DATA, event.getOriginal().getPersistentData().getCompound(DATA).copy());
        }
    }

    private static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && data(p).getLong("endsAt") > 0L) {
            finish(p, "Dungeon expedition ended on respawn.", Math.min(System.currentTimeMillis(), data(p).getLong("endsAt")));
        }
    }

    private static void travel(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer p && !TRANSFERRING.contains(p.getUUID())
            && (event.getDimension().equals(DIMENSION) || dungeon(p.level()))) {
            event.setCanceled(true);
            if (p.tickCount % 20 == 0) {
                message(p, "Use the dungeon portal to enter, or /dungeonworld leave to exit.");
            }
        }
    }

    private static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer p && dungeon(p.level()) && !operator(p)) {
            event.setCanceled(true);
        }
    }

    private static void placeBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && dungeon(p.level()) && !operator(p)
            && !event.getPlacedBlock().is(Blocks.LADDER) && !event.getPlacedBlock().is(Blocks.SCAFFOLDING)) {
            event.setCanceled(true);
        }
    }

    private static void explosion(ExplosionEvent.Detonate event) {
        if (dungeon(event.getLevel())) {
            event.getAffectedBlocks().clear();
        }
    }

    public static int createPortal(ServerPlayer p) {
        if (!p.level().dimension().equals(Level.OVERWORLD)) {
            message(p, "Create dungeon portals in the overworld.");
            return 0;
        }
        BlockPos base = p.blockPosition().offset(-1, 0, -4);
        ServerLevel level = p.serverLevel();
        for (int x = 0; x < 4; ++x) {
            for (int y = 0; y < 5; ++y) {
                for (int z = 0; z <= 2; ++z) {
                    if (level.getBlockState(base.offset(x, y, z)).canBeReplaced()) continue;
                    message(p, "Portal needs a clear 4 wide x 5 high x 3 deep space, four blocks north of you.");
                    return 0;
                }
            }
        }
        for (int x = 0; x < 4; ++x) {
            for (int z = 0; z <= 2; ++z) {
                level.setBlock(base.offset(x, -1, z), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
            }
        }
        for (int x = 0; x < 4; ++x) {
            for (int y = 0; y < 5; ++y) {
                level.setBlock(base.offset(x, y, 0), (x == 0 || x == 3 || y == 0 || y == 4 ? Blocks.CRYING_OBSIDIAN : PORTAL.get()).defaultBlockState(), 3);
            }
        }
        for (int x = 1; x <= 2; ++x) {
            for (int z = 1; z <= 2; ++z) {
                level.setBlock(base.offset(x, 0, z), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
            }
        }
        message(p, "Dungeon portal created. Enter from its south-facing apron.");
        return 1;
    }

    private static void network(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Timer.TYPE, Timer.CODEC, (packet, context) -> DungeonHudState.update(packet.seconds()));
    }

    public static void sync(ServerPlayer p, long end) {
        PacketDistributor.sendToPlayer(p, new Timer(end == 0L ? 0L : Math.max(1L, (end - System.currentTimeMillis() + 999L) / 1000L)));
    }

    static {
        BIOME_SOURCES.register("natural", () -> NaturalDungeonBiomeSource.CODEC);
        DENSITIES = DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, ID);
        DENSITIES.register("forest_mask", () -> ForestTerrainMask.CODEC);
        EXTRACTION = BLOCKS.registerSimpleBlock("extraction_marker", BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).lightLevel(state -> 15));
        PORTAL = BLOCKS.register("portal", () -> new PortalBlock(BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).noCollission().noOcclusion().lightLevel(state -> 12).noLootTable()));
        TRANSFERRING = new HashSet<>();
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        RUN_SECONDS = b.comment("Real seconds per expedition; continues while offline.").defineInRange("runSeconds", 3600, 10, 86400);
        COOLDOWN_SECONDS = b.comment("Real seconds after an expedition ends before re-entry.").defineInRange("cooldownSeconds", 14400, 0, 604800);
        SPEC = b.build();
    }

    public record Timer(long seconds) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Timer> TYPE = new CustomPacketPayload.Type<>(DungeonWorld.id("timer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Timer> CODEC = new StreamCodec<>() {
            public Timer decode(RegistryFriendlyByteBuf b) {
                return new Timer(b.readVarLong());
            }

            public void encode(RegistryFriendlyByteBuf b, Timer p) {
                b.writeVarLong(p.seconds());
            }
        };

        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static final class PortalBlock extends Block {
        private static final com.mojang.serialization.MapCodec<PortalBlock> CODEC = simpleCodec(PortalBlock::new);

        public PortalBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        protected com.mojang.serialization.MapCodec<? extends Block> codec() {
            return CODEC;
        }

        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (entity instanceof ServerPlayer p) {
                enter(p, pos);
            }
        }
    }
}
