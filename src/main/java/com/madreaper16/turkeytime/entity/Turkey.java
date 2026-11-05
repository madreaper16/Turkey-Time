package com.madreaper16.turkeytime.entity;

import com.madreaper16.turkeytime.TurkeyTime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import com.madreaper16.turkeytime.registry.TurkeySounds;
import com.madreaper16.turkeytime.registry.TurkeyItems;
import com.madreaper16.turkeytime.config.TurkeyConfig;
import com.madreaper16.turkeytime.entity.ai.TurkeyAvoidGoal;
import com.madreaper16.turkeytime.entity.ai.TurkeyFlockGoal;
import com.madreaper16.turkeytime.entity.ai.TurkeyDisplayGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Entity;
import java.util.ArrayList;
import java.util.List;

/** Basic passive species, independent of Chicken and its egg/jockey behaviors. */
public final class Turkey extends Animal {
    public static final int IDLE = 0, FORAGE = 1, STRUT = 2, FLUTTER = 3, STARTLED = 4;
    public static final int PRESENTATION_DURATION = 80;
    private static final EntityDataAccessor<Boolean> MALE = SynchedEntityData.defineId(Turkey.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> AUTUMN = SynchedEntityData.defineId(Turkey.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PLUMAGE = SynchedEntityData.defineId(Turkey.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DISPLAY = SynchedEntityData.defineId(Turkey.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DISPLAY_START = SynchedEntityData.defineId(Turkey.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> PANICKING = SynchedEntityData.defineId(Turkey.class, EntityDataSerializers.BOOLEAN);
    private PanicGoal panicGoal;
    private TurkeyAvoidGoal avoidGoal;
    public static final TagKey<Item> BREEDING_FOOD = TagKey.create(Registries.ITEM, TurkeyTime.id("turkey_food"));
    private int eggTime;
    private int flutterCooldown;
    private int gobbleCooldown;
    private int responseDelay;
    private int alarmCooldown;

    public Turkey(EntityType<? extends Turkey> type, Level level) {
        super(type, level);
        if (!level.isClientSide()) {
            entityData.set(MALE, random.nextBoolean());
            entityData.set(PLUMAGE, random.nextInt(3));
            entityData.set(AUTUMN, com.madreaper16.turkeytime.event.SeasonalEvent.active(((ServerLevel) level).getServer())
                    && random.nextDouble() < TurkeyConfig.get().seasonalCosmeticChance);
        }
        // Default female data may not fire a changed-data callback.
        refreshDimensions();
        eggTime = nextEggInterval();
    }

    private int nextEggInterval() {
        var config = TurkeyConfig.get();
        return config.eggLayIntervalMinTicks + random.nextInt(config.eggLayIntervalMaxTicks - config.eggLayIntervalMinTicks + 1);
    }

    public void setMale(boolean male) { entityData.set(MALE, male); refreshDimensions(); }
    public boolean isAutumn() { return entityData.get(AUTUMN); }
    public void setAutumn(boolean autumn) { entityData.set(AUTUMN, autumn); }
    public int getEggTime() { return eggTime; }
    public int getFlutterCooldown() { return flutterCooldown; }
    public int getGobbleCooldown() { return gobbleCooldown; }
    public int getResponseDelay() { return responseDelay; }

    /** At most sixteen nearby birds, even in dense farms. */
    public List<Turkey> neighbors(double radius) {
        List<Turkey> result = new ArrayList<>();
        level().getEntities(EntityTypeTest.forClass(Turkey.class), getBoundingBox().inflate(radius),
                t -> t != this && t.isAlive(), result, 16);
        return result;
    }

    public static final class FlockSpawnData extends AgeableMobGroupData {
        public FlockSpawnData() { super(false); }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData groupData) {
        if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) {
            var flock = groupData instanceof FlockSpawnData data ? data : new FlockSpawnData();
            setMale(flock.getGroupSize() % 3 == 1);
            groupData = flock;
        }
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MALE, false);
        builder.define(AUTUMN, false);
        builder.define(PLUMAGE, 0);
        builder.define(DISPLAY, IDLE);
        builder.define(DISPLAY_START, 0L);
        builder.define(PANICKING, false);
    }

    public boolean isMale() { return entityData.get(MALE); }
    public int getPlumage() { return entityData.get(PLUMAGE); }
    public int getDisplay() { return entityData.get(DISPLAY); }
    public long getDisplayStart() { return entityData.get(DISPLAY_START); }
    public boolean isPanicking() { return entityData.get(PANICKING); }

    @Override
    public float getAgeScale() {
        return super.getAgeScale() * (!isBaby() && !isMale() ? 0.9F : 1.0F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (MALE.equals(key)) refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Male", isMale());
        output.putBoolean("Autumn", isAutumn());
        output.putInt("Plumage", getPlumage());
        output.putInt("EggTime", eggTime);
        output.putInt("FlutterCooldown", flutterCooldown);
        output.putInt("GobbleCooldown", gobbleCooldown);
        // Transient displays deliberately reset after loading a save.
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(MALE, input.getBooleanOr("Male", isMale()));
        // Existing saves have a UUID; old saves without Autumn stay ordinary.
        // New /summon or spawner templates may load partial NBT without a UUID:
        // retain their server-rolled coat unless the template explicitly overrides it.
        entityData.set(AUTUMN, input.getBooleanOr("Autumn", input.getIntArray("UUID").isPresent() ? false : isAutumn()));
        entityData.set(PLUMAGE, Math.clamp(input.getIntOr("Plumage", getPlumage()), 0, 2));
        entityData.set(DISPLAY, IDLE);
        entityData.set(DISPLAY_START, 0L);
        entityData.set(PANICKING, false);
        refreshDimensions();
        eggTime = Math.clamp(input.getIntOr("EggTime", nextEggInterval()), 0, TurkeyConfig.get().eggLayIntervalMaxTicks);
        flutterCooldown = Math.clamp(input.getIntOr("FlutterCooldown", 0), 0, 200);
        gobbleCooldown = Math.clamp(input.getIntOr("GobbleCooldown", 0), 0, 600);
        responseDelay = 0;
    }

    /** Server-only preview API; never imparts velocity or grants flight. */
    public boolean startDisplay(int display) {
        if (level().isClientSide() || display < IDLE || display > STARTLED
                || (display == STRUT && (!isMale() || isBaby()))) return false;
        entityData.set(DISPLAY_START, level().getGameTime());
        entityData.set(DISPLAY, display);
        if (display == FLUTTER) playSound(TurkeySounds.FLUTTER, 0.35F, getVoicePitch());
        if (display == STRUT) gobble();
        if (display == STARTLED) playSound(TurkeySounds.ALARM, 0.4F, getVoicePitch());
        return true;
    }

    private void gobble() {
        if (gobbleCooldown > 0) return;
        gobbleCooldown = 600;
        playSound(TurkeySounds.GOBBLE, 0.4F, getVoicePitch());
        if (!TurkeyConfig.get().gobbleResponseEnabled) return;
        int replies = 0;
        for (Turkey other : neighbors(8)) {
            if (!other.isBaby() && other.gobbleCooldown == 0 && random.nextBoolean()) {
                other.gobbleCooldown = 600;
                other.responseDelay = 10 + random.nextInt(20);
                if (++replies == 2) break;
            }
        }
    }

    /** Small sub-block hop, no flight AI or blanket fall-damage immunity. */
    public void startle() {
        if (level().isClientSide()) return;
        if (alarmCooldown == 0) {
            playSound(TurkeySounds.ALARM, 0.35F, getVoicePitch());
            alarmCooldown = 120;
        }
        if (TurkeyConfig.get().escapeFlutterEnabled && flutterCooldown == 0 && onGround() && !isInWater()) {
            var velocity = getDeltaMovement();
            double angle = Math.toRadians(getYRot());
            setDeltaMovement(Math.clamp(velocity.x - Math.sin(angle) * 0.12, -0.3, 0.3), 0.24,
                    Math.clamp(velocity.z + Math.cos(angle) * 0.12, -0.3, 0.3));
            flutterCooldown = 200;
            startDisplay(FLUTTER);
        } else if (getDisplay() != FLUTTER) startDisplay(STARTLED);
    }

    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource source, float damage) {
        super.actuallyHurt(level, source, damage);
        if (isAlive()) startle();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            entityData.set(PANICKING, !isNoAi() && (panicGoal.isRunning() || avoidGoal.isFleeing()));
            if (flutterCooldown > 0) flutterCooldown--;
            if (gobbleCooldown > 0) gobbleCooldown--;
            if (alarmCooldown > 0) alarmCooldown--;
            if (responseDelay > 0 && --responseDelay == 0 && isAlive()) {
                // Replies play directly, so there is no recursive gobble chain.
                playSound(isMale() ? TurkeySounds.GOBBLE : TurkeySounds.AMBIENT, 0.3F, getVoicePitch());
            }
            if (isAlive() && !isMale() && !isBaby() && TurkeyConfig.get().eggLayingEnabled && --eggTime <= 0) {
                spawnAtLocation((ServerLevel) level(), TurkeyItems.TURKEY_EGG);
                eggTime = nextEggInterval();
            }
        }
        // Also expires on NoAI display mobs; AI goals are not required for cleanup.
        if (!level().isClientSide() && getDisplay() != IDLE && (isBaby() && getDisplay() == STRUT
                || level().getGameTime() - getDisplayStart() >= (getDisplay() == FLUTTER ? 20 : PRESENTATION_DURATION)
                || (isPanicking() && (getDisplay() == FORAGE || getDisplay() == STRUT)))) startDisplay(IDLE);
    }

    @Override
    protected SoundEvent getAmbientSound() { return TurkeySounds.AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return TurkeySounds.HURT; }
    @Override
    protected SoundEvent getDeathSound() { return TurkeySounds.DEATH; }
    @Override
    public int getAmbientSoundInterval() { return 240; }
    @Override
    protected float getSoundVolume() { return 0.35F; }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.TEMPT_RANGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        panicGoal = new PanicGoal(this, 1.25);
        goalSelector.addGoal(1, panicGoal);
        avoidGoal = new TurkeyAvoidGoal(this);
        goalSelector.addGoal(1, avoidGoal);
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.0, this::isFood, false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new TurkeyFlockGoal(this));
        goalSelector.addGoal(6, new TurkeyDisplayGoal(this));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(BREEDING_FOOD);
    }

    @Override
    public boolean canFallInLove() { return TurkeyConfig.get().breedingEnabled && super.canFallInLove(); }

    @Override
    public boolean canMate(Animal other) { return TurkeyConfig.get().breedingEnabled && super.canMate(other); }

    @Override
    public Turkey getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return TurkeyTime.TURKEY.create(level, EntitySpawnReason.BREEDING);
    }
}
