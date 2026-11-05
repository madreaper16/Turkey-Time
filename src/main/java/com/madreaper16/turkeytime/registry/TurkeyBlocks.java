package com.madreaper16.turkeytime.registry;

import com.madreaper16.turkeytime.TurkeyTime;
import com.madreaper16.turkeytime.block.CookedTurkeyBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class TurkeyBlocks {
    public static final CookedTurkeyBlock THANKSGIVING_FEAST = Registry.register(BuiltInRegistries.BLOCK,
            TurkeyTime.id("thanksgiving_feast"), new CookedTurkeyBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, TurkeyTime.id("thanksgiving_feast")))
                    .mapColor(MapColor.COLOR_BROWN).strength(0.5F).sound(SoundType.WOOD)
                    .noOcclusion().pushReaction(PushReaction.POPPED), true));
    public static final CookedTurkeyBlock COOKED_TURKEY = Registry.register(BuiltInRegistries.BLOCK,
            TurkeyTime.id("cooked_turkey"), new CookedTurkeyBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, TurkeyTime.id("cooked_turkey")))
                    .mapColor(MapColor.COLOR_BROWN).strength(0.5F).sound(SoundType.WOOL)
                    .noOcclusion().pushReaction(PushReaction.POPPED)));
    private TurkeyBlocks() {}
}
