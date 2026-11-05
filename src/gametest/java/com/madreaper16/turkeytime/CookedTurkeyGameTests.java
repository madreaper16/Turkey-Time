package com.madreaper16.turkeytime;

import com.madreaper16.turkeytime.block.CookedTurkeyBlock;
import com.madreaper16.turkeytime.registry.TurkeyBlocks;
import com.madreaper16.turkeytime.registry.TurkeyItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class CookedTurkeyGameTests {
    private static BlockPos place(GameTestHelper test, int bites) {
        test.setBlock(3, 0, 3, Blocks.STONE);
        test.setBlock(3, 1, 3, TurkeyBlocks.COOKED_TURKEY.defaultBlockState().setValue(CookedTurkeyBlock.BITES, bites));
        return test.absolutePos(new BlockPos(3, 1, 3));
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    private static int dropped(GameTestHelper test) {
        return test.getEntities(EntityTypes.ITEM).stream().map(e -> e.getItem())
                .filter(s -> s.is(TurkeyItems.COOKED_TURKEY)).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest
    public void sameFoodItemPlacesAndConsumesExactlyOne(GameTestHelper test) {
        test.setBlock(3, 0, 3, Blocks.STONE);
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(TurkeyItems.COOKED_TURKEY, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var support = test.absolutePos(new BlockPos(3, 0, 3));
        var result = stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(support)));
        test.assertTrue(result.consumesAction(), "Cooked turkey places through normal block-item use");
        test.assertTrue(test.getLevel().getBlockState(support.above()).is(TurkeyBlocks.COOKED_TURKEY), "Placed whole turkey exists");
        test.assertTrue(stack.getCount() == 1, "Placement consumes one item");
        test.assertTrue(TurkeyBlocks.COOKED_TURKEY.asItem() == TurkeyItems.COOKED_TURKEY, "Block maps to the existing food item");
        test.succeed();
    }

    @GameTest
    public void handheldTurkeyStillUsesNormalEating(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(4);
        var stack = new ItemStack(TurkeyItems.COOKED_TURKEY);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var result = stack.use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(result.consumesAction() && player.isUsingItem(), "Use in air starts the normal eating action");
        test.assertTrue(player.getUseItem().is(TurkeyItems.COOKED_TURKEY), "Player is eating cooked turkey");
        test.succeed();
    }

    @GameTest
    public void eightBitesEachGiveTwoHungerAndThenDisappear(GameTestHelper test) {
        var pos = place(test, 0);
        var level = test.getLevel();
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        int total = 0;
        for (int bite = 0; bite < 8; bite++) {
            var state = level.getBlockState(pos);
            test.assertTrue(state.getValue(CookedTurkeyBlock.BITES) == bite, "Correct visible bite state before serving " + bite);
            player.getFoodData().setFoodLevel(0);
            player.getFoodData().setSaturation(0);
            test.assertTrue(state.useWithoutItem(level, player, hit(pos)).consumesAction(), "Serving can be eaten");
            test.assertTrue(player.getFoodData().getFoodLevel() == 2, "Each serving restores one drumstick");
            total += player.getFoodData().getFoodLevel();
        }
        test.assertTrue(total == 16 && level.getBlockState(pos).isAir(), "Eight servings give sixteen total hunger and remove the block");
        test.assertTrue(dropped(test) == 0, "Eating the last serving creates no replacement item");
        test.succeed();
    }

    @GameTest
    public void fullPlayersDoNotConsumeBitesOrHeldItems(GameTestHelper test) {
        var pos = place(test, 0);
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(20);
        var held = new ItemStack(TurkeyItems.COOKED_TURKEY, 2);
        var state = test.getLevel().getBlockState(pos);
        state.useItemOn(held, test.getLevel(), player, InteractionHand.MAIN_HAND, hit(pos));
        test.assertTrue(test.getLevel().getBlockState(pos).getValue(CookedTurkeyBlock.BITES) == 0,
                "Full survival player leaves the roast untouched");
        player.getFoodData().setFoodLevel(8);
        state.useItemOn(held, test.getLevel(), player, InteractionHand.MAIN_HAND, hit(pos));
        test.assertTrue(player.getFoodData().getFoodLevel() == 10 && held.getCount() == 2,
                "Clicking the roast eats a serving without consuming a held food item");
        test.succeed();
    }

    @GameTest
    public void staleTwoPlayerClicksCannotDuplicateServings(GameTestHelper test) {
        var pos = place(test, 0);
        var level = test.getLevel();
        var first = test.makeMockPlayer(GameType.SURVIVAL);
        var second = test.makeMockPlayer(GameType.SURVIVAL);
        first.getFoodData().setFoodLevel(0); second.getFoodData().setFoodLevel(0);
        var stale = level.getBlockState(pos);
        stale.useWithoutItem(level, first, hit(pos));
        stale.useWithoutItem(level, second, hit(pos));
        test.assertTrue(level.getBlockState(pos).getValue(CookedTurkeyBlock.BITES) == 2,
                "Two clicks consume two distinct servings even with a stale snapshot");
        pos = place(test, 7);
        first.getFoodData().setFoodLevel(0); second.getFoodData().setFoodLevel(0);
        stale = level.getBlockState(pos);
        stale.useWithoutItem(level, first, hit(pos));
        stale.useWithoutItem(level, second, hit(pos));
        test.assertTrue(level.getBlockState(pos).isAir() && first.getFoodData().getFoodLevel() == 2
                && second.getFoodData().getFoodLevel() == 0, "Only one player receives the final serving");
        test.succeed();
    }

    @GameTest
    public void breakingCannotRefillPartiallyEatenTurkeys(GameTestHelper test) {
        var pos = place(test, 0);
        var level = test.getLevel();
        var intact = level.getBlockState(pos);
        test.assertTrue(Block.getDrops(intact, level, pos, null).stream().anyMatch(s -> s.is(TurkeyItems.COOKED_TURKEY)),
                "Untouched roast loot returns the food item");
        for (int bite = 1; bite < 8; bite++) {
            var partial = intact.setValue(CookedTurkeyBlock.BITES, bite);
            test.assertTrue(Block.getDrops(partial, level, pos, null).isEmpty(), "Partly eaten roast never drops a full replacement");
        }
        level.destroyBlock(pos, true);
        test.assertTrue(dropped(test) == 1, "Breaking an intact roast drops exactly one turkey");
        pos = place(test, 1);
        level.destroyBlock(pos, true);
        test.assertTrue(dropped(test) == 1, "Breaking a partially eaten roast adds no turkey");
        test.succeed();
    }

    @GameTest
    public void losingSupportUsesTheSameSafeDropRules(GameTestHelper test) {
        var pos = place(test, 0);
        var level = test.getLevel();
        level.destroyBlock(pos.below(), false);
        test.assertTrue(level.getBlockState(pos).isAir() && dropped(test) == 1, "Unsupported intact roast drops once");
        pos = place(test, 3);
        level.destroyBlock(pos.below(), false);
        test.assertTrue(level.getBlockState(pos).isAir() && dropped(test) == 1, "Unsupported partial roast drops no refill");
        test.succeed();
    }
}
