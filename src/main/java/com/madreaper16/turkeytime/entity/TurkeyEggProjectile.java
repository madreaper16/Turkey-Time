package com.madreaper16.turkeytime.entity;

import com.madreaper16.turkeytime.TurkeyTime;
import com.madreaper16.turkeytime.config.TurkeyConfig;
import com.madreaper16.turkeytime.registry.TurkeyItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A turkey egg can hatch one poult on the server; never a chicken or multiple babies. */
public class TurkeyEggProjectile extends ThrowableItemProjectile {
    public TurkeyEggProjectile(EntityType<? extends TurkeyEggProjectile> type, Level level) { super(type, level); }
    public TurkeyEggProjectile(Level level, LivingEntity owner, ItemStack stack) {
        super(TurkeyTime.TURKEY_EGG_PROJECTILE, owner, level, stack);
    }
    public TurkeyEggProjectile(Level level, double x, double y, double z, ItemStack stack) {
        super(TurkeyTime.TURKEY_EGG_PROJECTILE, x, y, z, level, stack);
    }

    @Override
    protected Item getDefaultItem() { return TurkeyItems.TURKEY_EGG; }

    protected boolean shouldHatch() { return random.nextInt(8) == 0; }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level() instanceof ServerLevel server) {
            hit.getEntity().hurtServer(server, damageSources().thrown(this, getOwner()), 0);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        if (isRemoved() || hit.getType() == HitResult.Type.MISS) return;
        super.onHit(hit);
        if (!(level() instanceof ServerLevel server)) return;
        if (TurkeyConfig.get().eggHatchingEnabled && shouldHatch()) {
            var poult = TurkeyTime.TURKEY.create(server, EntitySpawnReason.TRIGGERED);
            if (poult != null) {
                poult.setBaby(true);
                var pos = hit.getLocation();
                if (hit instanceof BlockHitResult block) {
                    var face = block.getDirection();
                    pos = pos.add(face.getStepX() * 0.25, face.getStepY() * 0.25, face.getStepZ() * 0.25);
                }
                poult.snapTo(pos);
                if (server.noCollision(poult) && server.addFreshEntity(poult)
                        && getOwner() instanceof net.minecraft.server.level.ServerPlayer player) {
                    com.madreaper16.turkeytime.advancement.TurkeyAdvancements.HATCHED_POULT.trigger(player);
                }
            }
        }
        server.broadcastEntityEvent(this, (byte) 3);
        discard();
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event != 3) { super.handleEntityEvent(event); return; }
        var particle = new ItemParticleOption(ParticleTypes.ITEM, getItem().getItem());
        for (int i = 0; i < 8; i++) level().addParticle(particle, getX(), getY(), getZ(),
                (random.nextFloat() - 0.5) * 0.08, random.nextFloat() * 0.08, (random.nextFloat() - 0.5) * 0.08);
    }
}
