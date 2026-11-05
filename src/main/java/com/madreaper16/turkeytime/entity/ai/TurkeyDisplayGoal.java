package com.madreaper16.turkeytime.entity.ai;

import com.madreaper16.turkeytime.config.TurkeyConfig;
import com.madreaper16.turkeytime.entity.Turkey;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public final class TurkeyDisplayGoal extends Goal {
    private final Turkey turkey;
    private int display;
    private int nextDisplayTick;
    public TurkeyDisplayGoal(Turkey turkey) {
        this.turkey = turkey;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override
    public boolean canUse() {
        if (turkey.tickCount < nextDisplayTick || turkey.tickCount % 40 != turkey.getId() % 40
                || turkey.isPanicking() || turkey.isInLove() || !turkey.onGround()
                || turkey.getDisplay() != Turkey.IDLE || turkey.getRandom().nextInt(8) != 0) return false;
        if (TurkeyConfig.get().maleStruttingEnabled && turkey.isMale() && !turkey.isBaby()
                && turkey.neighbors(6).stream().anyMatch(t -> !t.isMale() && !t.isBaby())) display = Turkey.STRUT;
        else if (TurkeyConfig.get().foragingEnabled) display = Turkey.FORAGE;
        else return false;
        return true;
    }
    @Override
    public void start() {
        turkey.getNavigation().stop();
        turkey.startDisplay(display);
        nextDisplayTick = turkey.tickCount + 400 + turkey.getRandom().nextInt(400);
    }
    @Override
    public boolean canContinueToUse() { return turkey.getDisplay() == display && !turkey.isInLove() && !turkey.isPanicking(); }
    @Override
    public void stop() { if (turkey.getDisplay() == display) turkey.startDisplay(Turkey.IDLE); }
}
