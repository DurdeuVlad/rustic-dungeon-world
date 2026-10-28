/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.biome.BiomeSource
 */
package dev.rustic.dungeonworld;

import dev.rustic.dungeonworld.NaturalDungeonBiomeSource;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;

public final class StructurePolicy {
    private StructurePolicy() {
    }

    public static boolean dungeonSource(BiomeSource source) {
        return source instanceof NaturalDungeonBiomeSource || !source.possibleBiomes().isEmpty() && source.possibleBiomes().stream().allMatch(b -> b.unwrapKey().map(k -> k.location().getNamespace().equals("rustic_dungeon_world")).orElse(false));
    }

    public static boolean blockedStructure(ResourceLocation id) {
        return id != null && (id.getNamespace().equals("minecraft") || id.getNamespace().equals("horror_element_mod"));
    }

    public static boolean blockedFeature(ResourceLocation id) {
        return id != null && (id.getNamespace().equals("horror_element_mod") || id.getNamespace().equals("minecraft") && Set.of("desert_well", "fossil_upper", "fossil_lower").contains(id.getPath()));
    }
}

