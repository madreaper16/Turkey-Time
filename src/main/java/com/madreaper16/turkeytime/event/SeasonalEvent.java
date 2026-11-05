package com.madreaper16.turkeytime.event;

import com.madreaper16.turkeytime.config.TurkeyConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.util.WeakHashMap;

/** Server calendar cache; no client date checks or network requests. */
public final class SeasonalEvent {
    private record Status(long nextCheck, boolean active) {}
    private static final WeakHashMap<MinecraftServer, Status> STATUS = new WeakHashMap<>();

    public static boolean activeOn(TurkeyConfig config, LocalDate date) {
        if (!config.enableSeasonalEvent) return false;
        var day = MonthDay.from(date);
        var start = MonthDay.parse("--" + config.seasonalEventStartMonthDay);
        var end = MonthDay.parse("--" + config.seasonalEventEndMonthDay);
        return start.compareTo(end) <= 0 ? day.compareTo(start) >= 0 && day.compareTo(end) <= 0
                : day.compareTo(start) >= 0 || day.compareTo(end) <= 0;
    }

    public static boolean active(MinecraftServer server) {
        var status = STATUS.get(server);
        if (status == null || server.getTickCount() >= status.nextCheck()) {
            var config = TurkeyConfig.get();
            var zone = config.seasonalEventTimezone.equals("system") ? ZoneId.systemDefault()
                    : ZoneId.of(config.seasonalEventTimezone);
            status = new Status((long) server.getTickCount() + 1200, activeOn(config, LocalDate.now(zone)));
            STATUS.put(server, status);
        }
        return status.active();
    }

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(SeasonalEvent::active);
        ServerTickEvents.END_SERVER_TICK.register(SeasonalEvent::active);
        ServerLifecycleEvents.SERVER_STOPPED.register(STATUS::remove);
    }
    private SeasonalEvent() {}
}
