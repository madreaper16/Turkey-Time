package com.madreaper16.turkeytime;

import com.madreaper16.turkeytime.config.TurkeyConfig;
import com.madreaper16.turkeytime.entity.Turkey;
import com.madreaper16.turkeytime.registry.TurkeyItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.ArrayList;
import java.util.List;

public final class TurkeyGameTests {
    private static void arena(GameTestHelper test) {
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) {
            test.setBlock(x, 0, z, Blocks.GRASS_BLOCK);
            if (x == 0 || x == 7 || z == 0 || z == 7) test.setBlock(x, 1, z, Blocks.OAK_FENCE);
        }
    }

    private static Turkey bird(GameTestHelper test, int x, int z, boolean male, boolean baby) {
        var turkey = test.spawn(TurkeyTime.TURKEY, x, 1, z);
        turkey.setMale(male);
        turkey.setBaby(baby);
        turkey.setNoAi(true);
        return turkey;
    }

    private static CompoundTag save(GameTestHelper test, Turkey bird) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, test.getLevel().registryAccess());
        bird.save(output);
        return output.buildResult();
    }

    private static void load(GameTestHelper test, Turkey bird, CompoundTag data) {
        bird.load(TagValueInput.create(ProblemReporter.DISCARDING, test.getLevel().registryAccess(), data));
    }

    private static int dropped(GameTestHelper test, net.minecraft.world.item.Item item) {
        return test.getEntities(net.minecraft.world.entity.EntityTypes.ITEM).stream().map(ItemEntity::getItem)
                .filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static boolean findsEscape(com.madreaper16.turkeytime.entity.ai.TurkeyAvoidGoal goal) {
        // Vanilla random destination selection can miss on an individual scan.
        for (int attempt = 0; attempt < 10; attempt++) if (goal.canUse()) return true;
        return false;
    }

    @GameTest
    public void configBoundsAndMalformedTypes(GameTestHelper test) {
        var config = TurkeyConfig.parse("{\"spawnWeight\":-9,\"minGroupSize\":99,\"maxGroupSize\":1,\"eggLayIntervalMinTicks\":-3,\"eggLayIntervalMaxTicks\":1}");
        test.assertTrue(config.spawnWeight == 1 && config.minGroupSize == 8 && config.maxGroupSize == 8,
                "Spawn values are bounded and ordered");
        test.assertTrue(config.eggLayIntervalMinTicks == 20 && config.eggLayIntervalMaxTicks == 20,
                "Egg intervals cannot be zero or inverted");
        for (String bad : List.of("null", "[]", "{", "{\"breedingEnabled\":\"false\"}",
                "{\"spawnWeight\":null}", "{\"spawnWeight\":2.5}", "{\"spawnWeight\":4294967296}")) {
            boolean rejected = false;
            try { TurkeyConfig.parse(bad); } catch (RuntimeException expected) { rejected = true; }
            test.assertTrue(rejected, "Reject malformed config: " + bad);
        }
        test.assertTrue(TurkeyConfig.parse("{}").eggLayIntervalMinTicks == 6000, "Missing settings retain defaults");
        test.succeed();
    }

    @GameTest
    public void eatingCookedTurkeyRestoresEightHunger(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(4);
        player.getFoodData().setSaturation(0);
        var stack = new ItemStack(TurkeyItems.COOKED_TURKEY);
        var food = stack.get(DataComponents.FOOD);
        test.assertTrue(food.nutrition() == 8, "Four drumsticks means eight hunger points");
        stack.get(DataComponents.CONSUMABLE).onConsume(test.getLevel(), player, stack);
        test.assertTrue(player.getFoodData().getFoodLevel() == 12, "Eating raises hunger from four to twelve");
        test.assertTrue(Math.abs(player.getFoodData().getSaturationLevel() - 9.6F) < 0.01,
                "Cooked turkey supplies 9.6 saturation");
        test.succeed();
    }

    @GameTest(maxTicks = 700, padding = 16)
    public void allThreeCookingBlocksActuallyCook(GameTestHelper test) {
        arena(test);
        test.setBlock(2, 1, 2, Blocks.FURNACE);
        test.setBlock(4, 1, 2, Blocks.SMOKER);
        test.setBlock(4, 1, 5, Blocks.CAMPFIRE);
        var furnace = (AbstractFurnaceBlockEntity) test.getLevel().getBlockEntity(test.absolutePos(new BlockPos(2, 1, 2)));
        var smoker = (AbstractFurnaceBlockEntity) test.getLevel().getBlockEntity(test.absolutePos(new BlockPos(4, 1, 2)));
        for (var block : List.of(furnace, smoker)) {
            block.setItem(0, new ItemStack(TurkeyItems.RAW_TURKEY));
            block.setItem(1, new ItemStack(Items.COAL));
        }
        var campfire = (CampfireBlockEntity) test.getLevel().getBlockEntity(test.absolutePos(new BlockPos(4, 1, 5)));
        test.assertTrue(campfire.placeFood(test.getLevel(), test.makeMockPlayer(GameType.SURVIVAL),
                new ItemStack(TurkeyItems.RAW_TURKEY)), "Campfire accepts raw turkey");
        test.runAtTickTime(650, () -> {
            test.assertTrue(furnace.getItem(2).is(TurkeyItems.COOKED_TURKEY), "Furnace produces cooked turkey");
            test.assertTrue(smoker.getItem(2).is(TurkeyItems.COOKED_TURKEY), "Smoker produces cooked turkey");
            test.assertTrue(dropped(test, TurkeyItems.COOKED_TURKEY) == 1, "Campfire ejects cooked turkey");
            test.succeed();
        });
    }

    @GameTest(maxTicks = 40, padding = 16)
    public void killingAnAdultActuallyDropsRawTurkey(GameTestHelper test) {
        arena(test);
        var adult = bird(test, 3, 3, true, false);
        adult.hurtServer(test.getLevel(), test.getLevel().damageSources().generic(), 100);
        test.runAfterDelay(2, () -> {
            int count = dropped(test, TurkeyItems.RAW_TURKEY);
            test.assertTrue(count >= 1 && count <= 2, "Killed adult drops one or two raw turkey");
            test.succeed();
        });
    }

    @GameTest(padding = 16)
    public void babyFireAndLootingUseTheRealLootTable(GameTestHelper test) {
        arena(test);
        var turkey = bird(test, 3, 3, true, false);
        var level = test.getLevel();
        var table = level.getServer().reloadableRegistries().getLootTable(turkey.getType().getDefaultLootTable().orElseThrow());
        var killer = test.makeMockPlayer(GameType.SURVIVAL);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, turkey)
                .withParameter(LootContextParams.ORIGIN, turkey.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
                .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer).create(LootContextParamSets.ENTITY);
        turkey.setBaby(true);
        test.assertTrue(table.getRandomItems(params, 16L).isEmpty(), "Baby loot table returns nothing");
        turkey.setBaby(false);
        turkey.igniteForTicks(100);
        var burning = table.getRandomItems(params, 16L);
        test.assertTrue(burning.stream().anyMatch(s -> s.is(TurkeyItems.COOKED_TURKEY))
                && burning.stream().noneMatch(s -> s.is(TurkeyItems.RAW_TURKEY)), "Burning adult meat is cooked");
        turkey.clearFire();
        int base = 0, looting = 0;
        for (int i = 1; i <= 100; i++) base += table.getRandomItems(params, (long) i).stream()
                .filter(s -> s.is(TurkeyItems.RAW_TURKEY)).mapToInt(ItemStack::getCount).sum();
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), 3);
        killer.setItemSlot(EquipmentSlot.MAINHAND, sword);
        for (int i = 1; i <= 100; i++) looting += table.getRandomItems(params, (long) i).stream()
                .filter(s -> s.is(TurkeyItems.RAW_TURKEY)).mapToInt(ItemStack::getCount).sum();
        test.assertTrue(looting > base, "Looting increases meat yield across deterministic samples");
        test.succeed();
    }

    @GameTest(padding = 16)
    public void onlyAdultHensLayAndTimersPersist(GameTestHelper test) {
        arena(test);
        var hen = bird(test, 2, 2, false, false);
        var tom = bird(test, 4, 2, true, false);
        var baby = bird(test, 4, 4, false, true);
        for (var bird : List.of(hen, tom, baby)) {
            var data = save(test, bird); data.putInt("EggTime", 0); load(test, bird, data);
        }
        var config = TurkeyConfig.get();
        boolean original = config.eggLayingEnabled;
        try {
            config.eggLayingEnabled = false;
            hen.tick();
            test.assertTrue(dropped(test, TurkeyItems.TURKEY_EGG) == 0, "Disabled laying produces no egg");
            config.eggLayingEnabled = true;
            hen.tick(); tom.tick(); baby.tick();
            test.assertTrue(dropped(test, TurkeyItems.TURKEY_EGG) == 1, "Only adult hen lays");
            int remaining = hen.getEggTime();
            test.assertTrue(remaining >= 6000 && remaining <= 12000, "Next laying interval uses configured defaults");
            var restored = bird(test, 2, 4, true, false);
            load(test, restored, save(test, hen));
            test.assertTrue(!restored.isMale() && restored.getEggTime() == remaining, "Sex and laying timer survive reload");
        } finally { config.eggLayingEnabled = original; }
        test.succeed();
    }

    @GameTest(maxTicks = 300, padding = 16)
    public void seedsBreedTurkeysIntoAPoult(GameTestHelper test) {
        arena(test);
        var first = bird(test, 2, 3, true, false);
        var second = bird(test, 4, 3, false, false);
        first.setNoAi(false); second.setNoAi(false);
        for (var food : List.of(Items.WHEAT_SEEDS, Items.PUMPKIN_SEEDS, Items.MELON_SEEDS, Items.BEETROOT_SEEDS))
            test.assertTrue(first.isFood(new ItemStack(food)), "Every specified seed is breeding food");
        var player = test.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS, 2));
        first.mobInteract(player, InteractionHand.MAIN_HAND);
        second.mobInteract(player, InteractionHand.MAIN_HAND);
        test.assertTrue(first.isInLove() && second.isInLove(), "Feeding seeds enters love mode");
        test.succeedWhen(() -> test.assertTrue(test.getEntities(TurkeyTime.TURKEY).stream().anyMatch(Turkey::isBaby),
                "Breeding produces a real poult"));
    }

    @GameTest
    public void naturalFlocksHaveMixedSexes(GameTestHelper test) {
        arena(test);
        SpawnGroupData flock = null;
        int males = 0;
        for (int i = 0; i < 5; i++) {
            var turkey = bird(test, 3, 3, true, false);
            flock = turkey.finalizeSpawn(test.getLevel(), test.getLevel().getCurrentDifficultyAt(turkey.blockPosition()),
                    EntitySpawnReason.NATURAL, flock);
            if (turkey.isMale()) males++;
        }
        test.assertTrue(males == 2, "Five-bird flock has two toms and three hens");
        test.succeed();
    }

    @GameTest
    public void habitatTablesIncludeOnlyTaggedBiomes(GameTestHelper test) {
        var biomes = test.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        for (String name : List.of("forest", "birch_forest", "old_growth_birch_forest", "plains", "sunflower_plains", "dappled_forest", "desert", "nether_wastes", "the_end")) {
            var biome = biomes.getOrThrow(ResourceKey.create(Registries.BIOME, net.minecraft.resources.Identifier.withDefaultNamespace(name))).value();
            var settings = biome.getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
            var turkey = settings.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream()
                    .filter(entry -> entry.value().type() == TurkeyTime.TURKEY).toList();
            boolean allowed = !List.of("desert", "nether_wastes", "the_end").contains(name);
            test.assertTrue(turkey.size() == (allowed ? 1 : 0), "Turkey habitat registration: " + name);
            if (allowed) test.assertTrue(turkey.getFirst().weight() == 8
                    && turkey.getFirst().value().count().minInclusive() == 2
                    && turkey.getFirst().value().count().maxInclusive() == 5, "Configured weight and group bounds");
        }
        test.succeed();
    }

    @GameTest(maxTicks = 80, padding = 16)
    public void hopAndGobbleCannotRepeatWithoutCooldown(GameTestHelper test) {
        arena(test);
        var leader = bird(test, 3, 3, true, false);
        List<Turkey> others = new ArrayList<>();
        for (int i = 0; i < 6; i++) others.add(bird(test, 4, 4, false, false));
        leader.startDisplay(Turkey.STRUT);
        long replies = others.stream().filter(t -> t.getResponseDelay() > 0).count();
        test.assertTrue(replies <= 2, "At most two initial gobble responses");
        int cooldown = leader.getGobbleCooldown();
        leader.startDisplay(Turkey.STRUT);
        test.assertTrue(leader.getGobbleCooldown() == cooldown, "Preview cannot bypass gobble cooldown");
        leader.setOnGround(true);
        leader.startle();
        test.assertTrue(Math.abs(leader.getDeltaMovement().y - 0.24) < 0.001
                && leader.getFlutterCooldown() == 200, "Escape is a short bounded hop");
        leader.setDeltaMovement(0, 0, 0);
        leader.startle();
        test.assertTrue(leader.getDeltaMovement().y == 0, "Cooldown prevents another hop");
        test.runAfterDelay(50, () -> {
            test.assertTrue(others.stream().filter(t -> t.getGobbleCooldown() > 0).count() <= 2,
                    "Gobble replies do not recurse into a chain");
            test.succeed();
        });
    }

    @GameTest(maxTicks = 600, padding = 16)
    public void flockCohesionClosesSeparation(GameTestHelper test) {
        arena(test);
        var first = bird(test, 2, 2, false, false);
        var second = bird(test, 6, 6, false, false);
        first.setNoAi(false); second.setNoAi(false);
        first.getRandom().setSeed(16); second.getRandom().setSeed(17);
        test.succeedWhen(() -> test.assertTrue(first.distanceToSqr(second) < 12.25,
                "Separated flock members approach within 3.5 blocks"));
    }

    @GameTest(maxTicks = 2400, padding = 16)
    public void naturalDisplayAIStartsForageOrStrut(GameTestHelper test) {
        arena(test);
        var tom = bird(test, 3, 3, true, false);
        bird(test, 5, 3, false, false);
        tom.setNoAi(false);
        tom.getRandom().setSeed(16);
        test.succeedWhen(() -> test.assertTrue(tom.getDisplay() == Turkey.FORAGE || tom.getDisplay() == Turkey.STRUT,
                "Natural display goal runs without a preview command"));
    }

    @GameTest(padding = 32)
    public void walkingPlayersAreSafeButSprintAndHostilesTriggerFlight(GameTestHelper test) {
        for (int x = -8; x < 17; x++) for (int z = -8; z < 17; z++) test.setBlock(x, 0, z, Blocks.GRASS_BLOCK);
        var turkey = bird(test, 3, 3, false, false);
        turkey.setOnGround(true);
        // The in-level helper hard-codes creative mode; use a survival mock here.
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var location = test.absolutePos(new BlockPos(5, 1, 3));
        player.setPos(location.getX(), location.getY(), location.getZ());
        test.getLevel().addFreshEntity(player);
        var goal = new com.madreaper16.turkeytime.entity.ai.TurkeyAvoidGoal(turkey);
        turkey.tickCount = turkey.getId() % 10;
        player.setSprinting(false);
        test.assertFalse(goal.canUse(), "Ordinary walking player does not startle turkey");
        player.setSprinting(true);
        test.assertTrue(findsEscape(goal), "Nearby sprinting survival player triggers an escape path");
        player.discard();
        var creative = test.makeMockPlayer(GameType.CREATIVE);
        creative.setPos(location.getX(), location.getY(), location.getZ());
        creative.setSprinting(true);
        test.getLevel().addFreshEntity(creative);
        test.assertFalse(goal.canUse(), "Creative player does not trigger escape");
        var zombie = test.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, 5, 1, 3);
        zombie.setNoAi(true);
        test.assertTrue(findsEscape(goal), "Nearby hostile mob triggers escape");
        test.succeed();
    }

    @GameTest(maxTicks = 48100, padding = 32)
    public void flockSurvivesTwoGameDaysWithoutTerrainDamage(GameTestHelper test) {
        arena(test);
        List<Turkey> flock = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            var turkey = bird(test, 2 + i % 4, 2 + i / 4, i % 3 == 0, i >= 6);
            turkey.setNoAi(false);
            turkey.getRandom().setSeed(1600 + i);
            flock.add(turkey);
        }
        test.runAtTickTime(48000, () -> {
            test.assertTrue(flock.stream().allMatch(t -> t.isAlive() && t.getHealth() == 8),
                    "All flock members survive two full game days at full health");
            test.assertTrue(flock.stream().noneMatch(Turkey::isBaby), "Poults mature after two game days");
            for (int x = 1; x < 7; x++) for (int z = 1; z < 7; z++)
                test.assertBlockPresent(Blocks.GRASS_BLOCK, x, 0, z);
            test.succeed();
        });
    }

    @GameTest(maxTicks = 400, padding = 16)
    public void heldSeedsTemptTurkeysTowardPlayers(GameTestHelper test) {
        arena(test);
        var turkey = bird(test, 2, 2, false, false);
        turkey.setNoAi(false);
        var player = test.makeMockServerPlayerInLevel();
        var target = test.absolutePos(new BlockPos(5, 1, 5));
        player.setPos(target.getX(), target.getY(), target.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS));
        test.assertTrue(turkey.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.TEMPT_RANGE) == 8,
                "Seed detection range is eight blocks");
        test.succeedWhen(() -> test.assertTrue(turkey.distanceToSqr(player) < 9,
                "Held seeds attract a turkey to within feeding range"));
    }

    @GameTest(maxTicks = 400, padding = 16)
    public void poultsFollowNearbyAdults(GameTestHelper test) {
        arena(test);
        var adult = bird(test, 2, 2, false, false);
        var poult = bird(test, 6, 6, false, true);
        poult.setNoAi(false);
        test.succeedWhen(() -> test.assertTrue(poult.distanceToSqr(adult) < 9,
                "Poult follows adult to within three blocks"));
    }
}
