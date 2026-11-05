package com.madreaper16.turkeytime.config;

import com.google.gson.GsonBuilder;
import com.madreaper16.turkeytime.TurkeyTime;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/** Startup-only server gameplay settings. Registries never disappear from existing saves. */
public final class TurkeyConfig {
    private static TurkeyConfig current = new TurkeyConfig();
    public boolean naturalSpawningEnabled = true;
    public int spawnWeight = 8;
    public int minGroupSize = 2;
    public int maxGroupSize = 5;
    public boolean breedingEnabled = true;
    public boolean eggLayingEnabled = true;
    public boolean eggHatchingEnabled = true;
    public int eggLayIntervalMinTicks = 6000;
    public int eggLayIntervalMaxTicks = 12000;
    public boolean fleeFromSprintingPlayers = true;
    public boolean fleeFromHostiles = true;
    public boolean escapeFlutterEnabled = true;
    public boolean flockingEnabled = true;
    public boolean foragingEnabled = true;
    public boolean gobbleResponseEnabled = true;
    public boolean maleStruttingEnabled = true;
    public boolean enableSeasonalEvent = true;
    public String seasonalEventStartMonthDay = "11-01";
    public String seasonalEventEndMonthDay = "11-30";
    public double seasonalCosmeticChance = 0.05;
    public String seasonalEventTimezone = "system";

    public static TurkeyConfig get() { return current; }

    public static TurkeyConfig parse(String json) {
        var gson = new GsonBuilder().setPrettyPrinting().create();
        var object = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        // Treat explicit nulls or wrong JSON primitive types as malformed, not false/zero.
        for (var field : TurkeyConfig.class.getFields()) {
            if (!object.has(field.getName())) continue;
            var value = object.get(field.getName());
            if (!value.isJsonPrimitive()) throw new IllegalArgumentException(field.getName());
            var primitive = value.getAsJsonPrimitive();
            if (field.getType() == boolean.class && !primitive.isBoolean()) throw new IllegalArgumentException(field.getName());
            if (field.getType() == String.class && !primitive.isString()) throw new IllegalArgumentException(field.getName());
            if (field.getType() == double.class && (!primitive.isNumber() || !Double.isFinite(primitive.getAsDouble())))
                throw new IllegalArgumentException(field.getName());
            if (field.getType() == int.class) {
                if (!primitive.isNumber()) throw new IllegalArgumentException(field.getName());
                primitive.getAsBigDecimal().intValueExact();
            }
        }
        TurkeyConfig result = gson.fromJson(object, TurkeyConfig.class);
        java.time.MonthDay.parse("--" + result.seasonalEventStartMonthDay);
        java.time.MonthDay.parse("--" + result.seasonalEventEndMonthDay);
        if (!result.seasonalEventTimezone.equals("system")) java.time.ZoneId.of(result.seasonalEventTimezone);
        result.seasonalCosmeticChance = Math.clamp(result.seasonalCosmeticChance, 0.0, 1.0);
        result.spawnWeight = Math.clamp(result.spawnWeight, 1, 100);
        result.minGroupSize = Math.clamp(result.minGroupSize, 2, 8);
        result.maxGroupSize = Math.clamp(result.maxGroupSize, result.minGroupSize, 8);
        result.eggLayIntervalMinTicks = Math.clamp(result.eggLayIntervalMinTicks, 20, 1728000);
        result.eggLayIntervalMaxTicks = Math.clamp(result.eggLayIntervalMaxTicks, result.eggLayIntervalMinTicks, 1728000);
        return result;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("turkeytime.json");
        var gson = new GsonBuilder().setPrettyPrinting().create();
        current = new TurkeyConfig();
        try {
            if (Files.exists(path)) current = parse(Files.readString(path));
            else {
                Files.createDirectories(path.getParent());
                Files.writeString(path, gson.toJson(current) + System.lineSeparator());
            }
        } catch (Exception error) {
            TurkeyTime.LOGGER.warn("Invalid/unreadable turkeytime.json; using safe defaults. Original file preserved.", error);
        }
    }
}
