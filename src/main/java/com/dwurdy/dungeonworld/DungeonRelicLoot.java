/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.advancements.critereon.LocationPredicate$Builder
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.level.storage.loot.LootPool
 *  net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer$Builder
 *  net.minecraft.world.level.storage.loot.entries.TagEntry
 *  net.minecraft.world.level.storage.loot.predicates.LocationCheck
 *  net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition
 *  net.minecraft.world.level.storage.loot.providers.number.ConstantValue
 *  net.minecraft.world.level.storage.loot.providers.number.NumberProvider
 *  net.neoforged.neoforge.event.LootTableLoadEvent
 */
package com.dwurdy.dungeonworld;

import com.dwurdy.dungeonworld.DungeonWorld;
import java.util.Set;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.neoforged.neoforge.event.LootTableLoadEvent;

public final class DungeonRelicLoot {
    static boolean target(ResourceLocation id) {
        return Set.of("dungeons_arise", "block_factorys_bosses").contains(id.getNamespace()) && id.getPath().startsWith("chests/") || id.getNamespace().equals("minecraft") && Set.of("chests/basic_chest", "chests/chest_level_1", "chests/chest_level_2", "chests/chest_level_3", "chests/farm_drop", "chests/firewell_d", "chests/shater").contains(id.getPath());
    }

    static void load(LootTableLoadEvent event) {
        if (!DungeonRelicLoot.target(event.getName())) {
            return;
        }
        float[] chances = new float[]{0.25f, 0.12f, 0.05f, 0.01f};
        for (int tier = 1; tier <= 4; ++tier) {
            event.getTable().addPool(LootPool.lootPool().name("dungeonworld_relic_tier_" + tier).setRolls((NumberProvider)ConstantValue.exactly((float)1.0f)).add((LootPoolEntryContainer.Builder)TagEntry.expandTag((TagKey)TagKey.create((ResourceKey)Registries.ITEM, (ResourceLocation)ResourceLocation.parse((String)("rpg_series:loot_tier/tier_" + tier + "_relics"))))).when(LocationCheck.checkLocation((LocationPredicate.Builder)LocationPredicate.Builder.location().setDimension(DungeonWorld.DIMENSION))).when(LootItemRandomChanceCondition.randomChance((float)chances[tier - 1])).build());
        }
    }
}

