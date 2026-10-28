/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.minecraft.core.Holder
 *  net.minecraft.core.HolderGetter
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.RegistryOps
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.biome.BiomeSource
 *  net.minecraft.world.level.biome.Climate$ParameterList
 *  net.minecraft.world.level.biome.Climate$ParameterPoint
 *  net.minecraft.world.level.biome.Climate$Sampler
 *  net.minecraft.world.level.biome.MultiNoiseBiomeSource
 *  net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList
 *  net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList$Preset
 */
package dev.rustic.dungeonworld;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.rustic.dungeonworld.DungeonWorld;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;

public final class NaturalDungeonBiomeSource
extends BiomeSource {
    public static final MapCodec<NaturalDungeonBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(RegistryOps.retrieveGetter(Registries.BIOME)).apply(i, NaturalDungeonBiomeSource::new));
    private final List<Holder<Biome>> choices;
    private final MultiNoiseBiomeSource terrainBiomes;
    private final List<Holder<Biome>> landChoices;

    private NaturalDungeonBiomeSource(HolderGetter<Biome> biomes) {
        Climate.ParameterList<Holder<Biome>> vanilla = (Climate.ParameterList<Holder<Biome>>)(Climate.ParameterList<?>)MultiNoiseBiomeSourceParameterList.knownPresets().get(MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD);
        TreeSet<String> names = new TreeSet<String>();
        names.addAll(List.of("crimson_forest", "warped_forest", "basalt_deltas", "desert", "ocean"));
        this.choices = names.stream().map(name -> (Holder<Biome>)biomes.getOrThrow(ResourceKey.create(Registries.BIOME, DungeonWorld.id(name)))).toList();
        this.terrainBiomes = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<Holder<Biome>>(vanilla.values().stream().map(pair -> Pair.of(pair.getFirst(), (Holder<Biome>)biomes.getOrThrow(ResourceKey.create(Registries.BIOME, DungeonWorld.id(((ResourceKey<Biome>)pair.getSecond()).location().getPath()))))).toList()));
        this.landChoices = this.choices.stream().filter(b -> !NaturalDungeonBiomeSource.terrainBound(((ResourceKey)b.unwrapKey().orElseThrow()).location().getPath())).toList();
    }

    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return this.choices.stream();
    }

    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        Holder terrain = this.terrainBiomes.getNoiseBiome(x, y, z, sampler);
        if (((ResourceKey)terrain.unwrapKey().orElseThrow()).location().getPath().contains("ocean")) {
            return this.choices.stream().filter(b -> ((ResourceKey)b.unwrapKey().orElseThrow()).location().getPath().equals("ocean")).findFirst().orElseThrow();
        }
        return this.landChoices.get(NaturalDungeonBiomeSource.regionIndex(x, z, this.landChoices.size()));
    }

    private static boolean terrainBound(String name) {
        return name.contains("ocean") || name.contains("river") || name.contains("beach") || name.equals("lush_caves") || name.equals("dripstone_caves") || name.equals("deep_dark");
    }

    static int regionIndex(int x, int z, int count) {
        int cellX = Math.floorDiv(x, 96);
        int cellZ = Math.floorDiv(z, 96);
        double best = Double.MAX_VALUE;
        int chosenX = cellX;
        int chosenZ = cellZ;
        for (int dx = -2; dx <= 2; ++dx) {
            for (int dz = -2; dz <= 2; ++dz) {
                double pz;
                int cx = cellX + dx;
                int cz = cellZ + dz;
                double px = (double)cx * 96.0 + 12.0 + (double)NaturalDungeonBiomeSource.indexFor(cx * 32, cz * 32, 7200) / 100.0;
                double distance = ((double)x - px) * ((double)x - px) + ((double)z - (pz = (double)cz * 96.0 + 12.0 + (double)NaturalDungeonBiomeSource.indexFor(cz * 32 + 3456, cx * 32 - 6784, 7200) / 100.0)) * ((double)z - pz);
                if (!(distance < best)) continue;
                best = distance;
                chosenX = cx;
                chosenZ = cz;
            }
        }
        return NaturalDungeonBiomeSource.indexFor(chosenX * 32, chosenZ * 32, count);
    }

    static int indexFor(int x, int z, int count) {
        long hash = (long)Math.floorDiv(x, 32) * -7046029254386353131L ^ (long)Math.floorDiv(z, 32) * -3335678366873096957L ^ 0x6A09E667F3BCC909L;
        hash = (hash ^ hash >>> 30) * -4658895280553007687L;
        hash = (hash ^ hash >>> 27) * -7723592293110705685L;
        hash ^= hash >>> 31;
        return (int)Long.remainderUnsigned(hash, count);
    }
}

