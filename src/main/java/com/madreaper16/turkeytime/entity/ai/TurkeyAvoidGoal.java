package com.madreaper16.turkeytime.entity.ai;

import com.madreaper16.turkeytime.config.TurkeyConfig;
import com.madreaper16.turkeytime.entity.Turkey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.EnumSet;

/** Non-combat threat detection also works in Peaceful; at most eight candidates. */
public final class TurkeyAvoidGoal extends Goal {
    private final Turkey turkey;
    private LivingEntity threat;
    private Path path;
    private boolean fleeing;
    public TurkeyAvoidGoal(Turkey turkey) {
        this.turkey = turkey;
        setFlags(EnumSet.of(Flag.MOVE));
    }
    private static boolean isThreat(LivingEntity entity) {
        if (entity instanceof Player player) return TurkeyConfig.get().fleeFromSprintingPlayers
                && player.isSprinting() && !player.isCreative() && !player.isSpectator();
        return TurkeyConfig.get().fleeFromHostiles && entity instanceof Enemy;
    }
    @Override
    public boolean canUse() {
        if (turkey.tickCount % 10 != turkey.getId() % 10) return false;
        var candidates = new ArrayList<LivingEntity>();
        turkey.level().getEntities(EntityTypeTest.forClass(LivingEntity.class),
                turkey.getBoundingBox().inflate(6, 3, 6),
                entity -> entity.isAlive() && isThreat(entity) && turkey.distanceToSqr(entity) < 36,
                candidates, 8);
        threat = candidates.stream().filter(entity -> turkey.getSensing().hasLineOfSight(entity))
                .min(java.util.Comparator.comparingDouble(turkey::distanceToSqr)).orElse(null);
        if (threat == null) return false;
        var destination = DefaultRandomPos.getPosAway(turkey, 12, 4, threat.position());
        if (destination == null || threat.distanceToSqr(destination) < threat.distanceToSqr(turkey)) return false;
        path = turkey.getNavigation().createPath(destination.x, destination.y, destination.z, 0);
        return path != null;
    }
    @Override
    public boolean canContinueToUse() {
        return threat != null && threat.isAlive() && isThreat(threat) && turkey.distanceToSqr(threat) < 144
                && !turkey.getNavigation().isDone();
    }
    @Override
    public void start() {
        turkey.getNavigation().moveTo(path, 1.35);
        fleeing = true;
        turkey.startle();
    }
    @Override
    public void stop() {
        turkey.getNavigation().stop();
        fleeing = false;
        threat = null;
        path = null;
    }
    @Override
    public void tick() {
        turkey.getNavigation().setSpeedModifier(turkey.distanceToSqr(threat) < 49 ? 1.35 : 1.15);
    }
    public boolean isFleeing() { return fleeing; }
}
