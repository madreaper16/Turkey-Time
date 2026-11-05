package com.madreaper16.turkeytime.registry;

import com.madreaper16.turkeytime.TurkeyTime;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public final class TurkeySounds {
    public static final SoundEvent AMBIENT = register("entity.turkey.ambient");
    public static final SoundEvent GOBBLE = register("entity.turkey.gobble");
    public static final SoundEvent ALARM = register("entity.turkey.alarm");
    public static final SoundEvent HURT = register("entity.turkey.hurt");
    public static final SoundEvent DEATH = register("entity.turkey.death");
    public static final SoundEvent FLUTTER = register("entity.turkey.flutter");

    private static SoundEvent register(String name) {
        var id = TurkeyTime.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void initialize() { /* Trigger common registration. */ }
    private TurkeySounds() {}
}
