package com.madreaper16.turkeytime;

import com.madreaper16.turkeytime.block.CookedTurkeyBlock;
import com.madreaper16.turkeytime.config.TurkeyConfig;
import com.madreaper16.turkeytime.entity.Turkey;
import com.madreaper16.turkeytime.entity.TurkeyEggProjectile;
import com.madreaper16.turkeytime.registry.TurkeyBlocks;
import com.madreaper16.turkeytime.registry.TurkeyItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class EggAndFeastGameTests {
    private static void floor(GameTestHelper test) {
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) test.setBlock(x, 0, z, Blocks.STONE);
    }

    private static final class ControlledEgg extends TurkeyEggProjectile {
        private final boolean hatch;
        ControlledEgg(ServerLevel level, boolean hatch) {
            super(TurkeyTime.TURKEY_EGG_PROJECTILE, level);
            this.hatch = hatch;
            setItem(new ItemStack(TurkeyItems.TURKEY_EGG));
        }
        @Override protected boolean shouldHatch() { return hatch; }
        void impact(HitResult hit) { onHit(hit); }
        void seed(long seed) { random.setSeed(seed); }
        boolean naturalRoll() { return super.shouldHatch(); }
    }

    private static BlockHitResult floorHit(GameTestHelper test) {
        var block = test.absolutePos(new BlockPos(3, 0, 3));
        return new BlockHitResult(Vec3.atCenterOf(block).add(0, 0.5, 0), Direction.UP, block, false);
    }

    @GameTest(padding = 16)
    public void throwingUsesOneEggAndCorrectProjectile(GameTestHelper test) {
        floor(test);
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var origin = test.absolutePos(new BlockPos(3, 2, 3));
        player.setPos(origin.getX(), origin.getY(), origin.getZ());
        var stack = new ItemStack(TurkeyItems.TURKEY_EGG, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        test.assertTrue(stack.use(test.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "Use throws a turkey egg");
        var eggs = test.getEntities(TurkeyTime.TURKEY_EGG_PROJECTILE);
        test.assertTrue(stack.getCount() == 1 && eggs.size() == 1, "One item produces one server projectile");
        test.assertTrue(eggs.getFirst().getItem().is(TurkeyItems.TURKEY_EGG)
                && eggs.getFirst().getItem().getCount() == 1, "Projectile carries the distinct turkey egg visual");
        test.assertTrue(test.getEntities(TurkeyTime.TURKEY).isEmpty(), "No baby before impact");
        test.succeed();
    }

    @GameTest(padding = 16)
    public void successfulImpactCreatesOnePoultOnlyOnce(GameTestHelper test) {
        floor(test);
        var egg = new ControlledEgg(test.getLevel(), true);
        egg.snapTo(floorHit(test).getLocation());
        test.getLevel().addFreshEntity(egg);
        egg.impact(floorHit(test));
        egg.impact(floorHit(test));
        var poults = test.getEntities(TurkeyTime.TURKEY);
        test.assertTrue(egg.isRemoved() && poults.size() == 1 && poults.getFirst().isBaby(),
                "Impact creates exactly one baby, even if called again");
        test.assertTrue(poults.getFirst().getAge() == -24000, "Poult starts with normal baby growth age");
        test.assertTrue(test.getEntities(EntityTypes.CHICKEN).isEmpty(), "Turkey eggs never hatch chickens");
        test.succeed();
    }

    @GameTest(padding = 16)
    public void failedAndDisabledHatchesStillConsumeProjectile(GameTestHelper test) {
        floor(test);
        var failed = new ControlledEgg(test.getLevel(), false);
        failed.impact(floorHit(test));
        test.assertTrue(failed.isRemoved() && test.getEntities(TurkeyTime.TURKEY).isEmpty(), "Failed roll shatters without a poult");
        boolean original = TurkeyConfig.get().eggHatchingEnabled;
        try {
            TurkeyConfig.get().eggHatchingEnabled = false;
            var disabled = new ControlledEgg(test.getLevel(), true);
            disabled.impact(floorHit(test));
            test.assertTrue(disabled.isRemoved() && test.getEntities(TurkeyTime.TURKEY).isEmpty(),
                    "Hatching can be disabled independently of throwing");
        } finally { TurkeyConfig.get().eggHatchingEnabled = original; }
        test.succeed();
    }

    @GameTest(padding = 16)
    public void blockedImpactCannotSpawnInsideSolidBlocks(GameTestHelper test) {
        floor(test);
        test.setBlock(3, 1, 3, Blocks.STONE);
        var egg = new ControlledEgg(test.getLevel(), true);
        egg.impact(floorHit(test));
        test.assertTrue(egg.isRemoved() && test.getEntities(TurkeyTime.TURKEY).isEmpty(),
                "Collision check prevents a poult inside a solid block");
        test.succeed();
    }

    @GameTest
    public void naturalHatchChanceIsSmallAndNonzero(GameTestHelper test) {
        var egg = new ControlledEgg(test.getLevel(), false);
        egg.seed(16);
        int successes = 0;
        for (int i = 0; i < 1600; i++) if (egg.naturalRoll()) successes++;
        test.assertTrue(successes >= 150 && successes <= 250, "Seeded rolls approximate the specified one-in-eight chance: " + successes);
        test.succeed();
    }

    @GameTest(maxTicks = 35, padding = 16)
    public void projectilePhysicsActuallyHitsAndRemovesEgg(GameTestHelper test) {
        floor(test);
        var origin = test.absolutePos(new BlockPos(3, 3, 3));
        var egg = new TurkeyEggProjectile(test.getLevel(), origin.getX()+0.5, origin.getY(), origin.getZ()+0.5,
                new ItemStack(TurkeyItems.TURKEY_EGG));
        test.getLevel().addFreshEntity(egg);
        egg.shoot(0, -1, 0, 1, 0);
        test.runAtTickTime(20, () -> {
            test.assertTrue(egg.isRemoved(), "A real moving egg collides with the floor and shatters");
            test.assertTrue(test.getEntities(TurkeyTime.TURKEY).size() <= 1
                    && test.getEntities(TurkeyTime.TURKEY).stream().allMatch(Turkey::isBaby), "Real impact spawns at most one poult");
            test.succeed();
        });
    }

    @GameTest(maxTicks = 15, padding = 32)
    public void dispenserShootsTurkeyEggInsteadOfDroppingIt(GameTestHelper test) {
        floor(test);
        test.setBlock(2, 1, 2, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH));
        var dispenser = (DispenserBlockEntity) test.getLevel().getBlockEntity(test.absolutePos(new BlockPos(2, 1, 2)));
        dispenser.setItem(0, new ItemStack(TurkeyItems.TURKEY_EGG, 2));
        test.setBlock(2, 2, 2, Blocks.REDSTONE_BLOCK);
        test.runAtTickTime(6, () -> {
            test.assertTrue(dispenser.getItem(0).getCount() == 1, "Dispenser consumes one egg");
            var eggs = test.getLevel().getEntitiesOfClass(TurkeyEggProjectile.class, test.getBounds().inflate(12));
            test.assertTrue(eggs.size() == 1, "Dispenser creates the custom projectile");
            test.succeed();
        });
    }

    @GameTest(maxTicks = 280, padding = 16)
    public void killCookCraftPlaceAndShareTheWholeFeast(GameTestHelper test) {
        floor(test);
        var adult = test.spawn(TurkeyTime.TURKEY, 5, 1, 5);
        adult.setBaby(false); adult.setNoAi(true);
        adult.hurtServer(test.getLevel(), test.getLevel().damageSources().generic(), 100);
        var meat = test.getEntities(EntityTypes.ITEM).stream().filter(e -> e.getItem().is(TurkeyItems.RAW_TURKEY))
                .findFirst().orElseThrow();
        var raw = meat.getItem().split(1);
        if (meat.getItem().isEmpty()) meat.discard();
        test.setBlock(1, 1, 1, Blocks.FURNACE);
        var furnace = (AbstractFurnaceBlockEntity) test.getLevel().getBlockEntity(test.absolutePos(new BlockPos(1, 1, 1)));
        furnace.setItem(0, raw); furnace.setItem(1, new ItemStack(Items.COAL));
        test.runAtTickTime(240, () -> {
            test.assertTrue(furnace.getItem(2).is(TurkeyItems.COOKED_TURKEY), "Killed turkey meat cooks in the real furnace");
            var input = CraftingInput.of(3, 3, List.of(new ItemStack(Items.BREAD),
                    furnace.getItem(2).copyWithCount(1), new ItemStack(Items.CARROT),
                    new ItemStack(Items.OAK_SLAB), new ItemStack(Items.CHERRY_SLAB), new ItemStack(Items.CRIMSON_SLAB),
                    ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
            var recipe = test.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, test.getLevel()).orElseThrow();
            var feast = recipe.value().assemble(input);
            test.assertTrue(feast.is(TurkeyItems.THANKSGIVING_FEAST) && feast.getCount() == 1, "Survival ingredients craft one feast");
            var wrong = CraftingInput.of(3, 2, List.of(new ItemStack(Items.BREAD),
                    new ItemStack(TurkeyItems.COOKED_TURKEY), new ItemStack(Items.CARROT),
                    new ItemStack(Items.OAK_SLAB), new ItemStack(Items.STONE_SLAB), new ItemStack(Items.OAK_SLAB)));
            test.assertTrue(test.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, wrong, test.getLevel()).isEmpty(),
                    "Wrong ingredient does not craft a feast");
            var first = test.makeMockPlayer(GameType.SURVIVAL);
            var second = test.makeMockPlayer(GameType.SURVIVAL);
            first.setItemInHand(InteractionHand.MAIN_HAND, feast);
            var support = test.absolutePos(new BlockPos(3, 0, 3));
            var hit = new BlockHitResult(Vec3.atCenterOf(support).add(0,0.5,0), Direction.UP, support, false);
            test.assertTrue(feast.useOn(new UseOnContext(first, InteractionHand.MAIN_HAND, hit)).consumesAction()
                    && feast.isEmpty(), "One crafted feast places once");
            var pos = support.above();
            int total = 0;
            for (int bite = 0; bite < 8; bite++) {
                var player = bite % 2 == 0 ? first : second;
                player.getFoodData().setFoodLevel(0);
                var state = test.getLevel().getBlockState(pos);
                test.assertTrue(state.is(TurkeyBlocks.THANKSGIVING_FEAST)
                        && state.getValue(CookedTurkeyBlock.BITES) == bite, "Feast serving state is correct");
                state.useWithoutItem(test.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                test.assertTrue(player.getFoodData().getFoodLevel() == 2, "Shared serving restores two hunger");
                total += player.getFoodData().getFoodLevel();
            }
            test.assertTrue(total == 16 && test.getLevel().getBlockState(pos).isAir(), "Players share all eight servings without a replacement");
            test.succeed();
        });
    }

    @GameTest
    public void feastRecipeRequiresRowsAndAcceptsEveryWoodenSlab(GameTestHelper test) {
        var woods = List.of(Items.OAK_SLAB, Items.SPRUCE_SLAB, Items.BIRCH_SLAB, Items.JUNGLE_SLAB,
                Items.ACACIA_SLAB, Items.DARK_OAK_SLAB, Items.PALE_OAK_SLAB, Items.CRIMSON_SLAB,
                Items.WARPED_SLAB, Items.MANGROVE_SLAB, Items.BAMBOO_SLAB, Items.CHERRY_SLAB, Items.POPLAR_SLAB);
        for (var wood : woods) {
            var input = CraftingInput.of(3, 2, List.of(new ItemStack(Items.BREAD),
                    new ItemStack(TurkeyItems.COOKED_TURKEY), new ItemStack(Items.CARROT),
                    new ItemStack(wood), new ItemStack(wood), new ItemStack(wood)));
            var recipe = test.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, test.getLevel());
            test.assertTrue(recipe.isPresent() && recipe.get().value().assemble(input).is(TurkeyItems.THANKSGIVING_FEAST),
                    "Every vanilla wooden slab can form the tray: " + wood);
        }
        var misplaced = CraftingInput.of(3, 2, List.of(new ItemStack(TurkeyItems.COOKED_TURKEY),
                new ItemStack(Items.BREAD), new ItemStack(Items.CARROT),
                new ItemStack(Items.OAK_SLAB), new ItemStack(Items.OAK_SLAB), new ItemStack(Items.OAK_SLAB)));
        test.assertTrue(test.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, misplaced, test.getLevel()).isEmpty(),
                "Turkey must be in the middle of the food row");
        var old = CraftingInput.of(2, 2, List.of(new ItemStack(TurkeyItems.COOKED_TURKEY),
                new ItemStack(Items.BREAD), new ItemStack(Items.CARROT), new ItemStack(Items.BOWL)));
        test.assertTrue(test.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, old, test.getLevel()).isEmpty(),
                "The old shapeless bowl recipe no longer works");
        test.succeed();
    }

    @GameTest
    public void feastBreakRulesAndLastServingAreSafe(GameTestHelper test) {
        floor(test);
        var pos = test.absolutePos(new BlockPos(3, 1, 3));
        var intact = TurkeyBlocks.THANKSGIVING_FEAST.defaultBlockState();
        test.setBlock(3,1,3,intact);
        test.assertTrue(Block.getDrops(intact,test.getLevel(),pos,null).stream().anyMatch(s->s.is(TurkeyItems.THANKSGIVING_FEAST)),
                "Untouched feast drops its own item");
        for (int bites=1;bites<8;bites++) test.assertTrue(Block.getDrops(intact.setValue(CookedTurkeyBlock.BITES,bites),
                test.getLevel(),pos,null).isEmpty(), "Partial feast never refunds a full meal");
        var last = intact.setValue(CookedTurkeyBlock.BITES,7);
        test.setBlock(3,1,3,last);
        var a=test.makeMockPlayer(GameType.SURVIVAL); var b=test.makeMockPlayer(GameType.SURVIVAL);
        a.getFoodData().setFoodLevel(0); b.getFoodData().setFoodLevel(0);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        last.useWithoutItem(test.getLevel(),a,hit); last.useWithoutItem(test.getLevel(),b,hit);
        test.assertTrue(a.getFoodData().getFoodLevel()==2 && b.getFoodData().getFoodLevel()==0
                && test.getLevel().getBlockState(pos).isAir(), "Final feast serving can only be claimed once");
        test.succeed();
    }
}
