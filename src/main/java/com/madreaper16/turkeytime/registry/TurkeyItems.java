package com.madreaper16.turkeytime.registry;

import com.madreaper16.turkeytime.TurkeyTime;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import com.madreaper16.turkeytime.item.TurkeyEggItem;

public final class TurkeyItems {
    public static final Item RAW_TURKEY = register("raw_turkey", new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build()));
    // Cooked turkey restores eight hunger points, or four drumsticks.
    public static final Item COOKED_TURKEY = Registry.register(BuiltInRegistries.ITEM, TurkeyTime.id("cooked_turkey"),
            new BlockItem(TurkeyBlocks.COOKED_TURKEY, new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, TurkeyTime.id("cooked_turkey")))
                    .food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.6F).build())));
    public static final Item TURKEY_EGG = Registry.register(BuiltInRegistries.ITEM, TurkeyTime.id("turkey_egg"),
            new TurkeyEggItem(new Item.Properties().stacksTo(16)
                    .setId(ResourceKey.create(Registries.ITEM, TurkeyTime.id("turkey_egg")))));
    public static final Item THANKSGIVING_FEAST = Registry.register(BuiltInRegistries.ITEM, TurkeyTime.id("thanksgiving_feast"),
            new BlockItem(TurkeyBlocks.THANKSGIVING_FEAST, new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, TurkeyTime.id("thanksgiving_feast")))));

    private static Item register(String name, Item.Properties properties) {
        var id = TurkeyTime.id(name);
        return Registry.register(BuiltInRegistries.ITEM, id,
                new Item(properties.setId(ResourceKey.create(Registries.ITEM, id))));
    }
    public static void initialize() {}
    private TurkeyItems() {}
}
