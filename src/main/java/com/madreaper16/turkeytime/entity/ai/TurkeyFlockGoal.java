package com.madreaper16.turkeytime.entity.ai;

import com.madreaper16.turkeytime.config.TurkeyConfig;
import com.madreaper16.turkeytime.entity.Turkey;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

/** Bounded local cohesion, no persistent flock owner or whole-world scan. */
public final class TurkeyFlockGoal extends Goal {
    private final Turkey turkey;
    private Turkey neighbor;
    private int ticks;
    public TurkeyFlockGoal(Turkey turkey) { this.turkey = turkey; setFlags(EnumSet.of(Flag.MOVE)); }
    @Override
    public boolean canUse() {
        if (!TurkeyConfig.get().flockingEnabled || turkey.isBaby() || turkey.isInLove()
                || turkey.tickCount % 80 != turkey.getId() % 80) return false;
        neighbor = turkey.neighbors(10).stream().filter(t -> !t.isBaby() && turkey.distanceToSqr(t) > 16)
                .min(java.util.Comparator.comparingDouble(turkey::distanceToSqr)).orElse(null);
        return neighbor != null;
    }
    @Override
    public void start() { ticks = 0; }
    @Override
    public boolean canContinueToUse() {
        return neighbor != null && neighbor.isAlive() && ticks < 80
                && turkey.distanceToSqr(neighbor) > 9 && turkey.distanceToSqr(neighbor) < 144;
    }
    @Override
    public void tick() {
        if (ticks++ % 20 == 0) turkey.getNavigation().moveTo(neighbor, 1.0);
    }
    @Override
    public void stop() { turkey.getNavigation().stop(); neighbor = null; }
}
