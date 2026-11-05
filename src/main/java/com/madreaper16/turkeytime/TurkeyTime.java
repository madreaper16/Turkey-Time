package com.madreaper16.turkeytime;

import com.madreaper16.turkeytime.entity.Turkey;
import com.madreaper16.turkeytime.entity.TurkeyEggProjectile;
import net.minecraft.world.level.block.DispenserBlock;
import com.madreaper16.turkeytime.registry.TurkeySounds;
import com.madreaper16.turkeytime.command.TurkeyCommands;
import com.madreaper16.turkeytime.registry.TurkeyItems;
import com.madreaper16.turkeytime.config.TurkeyConfig;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TurkeyTime implements ModInitializer {
    public static final String MOD_ID = "turkeytime";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final EntityType<Turkey> TURKEY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, id("turkey"),
            EntityType.Builder.of(Turkey::new, MobCategory.CREATURE)
                    .sized(0.7F, 0.9F).eyeHeight(0.75F).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id("turkey"))));
    public static final Item TURKEY_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM, id("turkey_spawn_egg"),
            new SpawnEggItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id("turkey_spawn_egg")))
                    .spawnEgg(TURKEY)));
    public static final EntityType<TurkeyEggProjectile> TURKEY_EGG_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, id("turkey_egg"),
            EntityType.Builder.<TurkeyEggProjectile>of(TurkeyEggProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id("turkey_egg"))));

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        TurkeyConfig.load();
        com.madreaper16.turkeytime.advancement.TurkeyAdvancements.initialize();
        com.madreaper16.turkeytime.event.SeasonalEvent.initialize();
        TurkeyItems.initialize();
        DispenserBlock.registerProjectileBehavior(TurkeyItems.TURKEY_EGG);
        TurkeySounds.initialize();
        TurkeyCommands.register();
        FabricDefaultAttributeRegistry.register(TURKEY, Turkey.createAttributes());
        SpawnPlacements.register(TURKEY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getLevel().dimension() == Level.OVERWORLD
                        && Animal.checkAnimalSpawnRules(type, level, reason, pos, random));
        var config = TurkeyConfig.get();
        if (config.naturalSpawningEnabled) {
            TagKey<Biome> biomes = TagKey.create(Registries.BIOME, id("turkey_spawns"));
            // The tag controls habitat; the spawn predicate above enforces dimension.
            BiomeModifications.addSpawn(BiomeSelectors.tag(biomes),
                    MobCategory.CREATURE, TURKEY, config.spawnWeight, config.minGroupSize, config.maxGroupSize);
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries ->
                entries.accept(new ItemStack(TURKEY_SPAWN_EGG), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
            entries.accept(new ItemStack(TurkeyItems.RAW_TURKEY), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            entries.accept(new ItemStack(TurkeyItems.COOKED_TURKEY), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            entries.accept(new ItemStack(TurkeyItems.THANKSGIVING_FEAST), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries ->
                entries.accept(new ItemStack(TurkeyItems.TURKEY_EGG), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        LOGGER.info("Turkey Time initialized; natural spawning={}, spawn weight={}", config.naturalSpawningEnabled, config.spawnWeight);
    }
}
