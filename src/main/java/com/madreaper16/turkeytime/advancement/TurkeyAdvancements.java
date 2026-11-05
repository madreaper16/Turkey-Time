package com.madreaper16.turkeytime.advancement;

import com.madreaper16.turkeytime.TurkeyTime;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** Fired only after a player-owned egg successfully adds a poult to the server. */
public final class TurkeyAdvancements {
    public static final PlayerTrigger HATCHED_POULT = Registry.register(BuiltInRegistries.TRIGGER_TYPES,
            TurkeyTime.id("hatched_poult"), new PlayerTrigger());
    public static void initialize() {}
    private TurkeyAdvancements() {}
}
