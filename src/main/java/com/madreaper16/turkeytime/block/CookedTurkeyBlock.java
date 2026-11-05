package com.madreaper16.turkeytime.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/** Eight server-authoritative servings of the same edible cooked-turkey item. */
public final class CookedTurkeyBlock extends HorizontalDirectionalBlock {
    public static final int SERVINGS = 8;
    public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, SERVINGS - 1);
    private static final double[] CUTS = {1, 4, 5.5, 6.75, 8, 9.25, 10.5, 12};
    private final boolean platter;

    public CookedTurkeyBlock(Properties properties) {
        this(properties, false);
    }

    public CookedTurkeyBlock(Properties properties, boolean platter) {
        super(properties);
        this.platter = platter;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BITES, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BITES);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return eat(level, pos, player);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return eat(level, pos, player);
    }

    private InteractionResult eat(Level level, BlockPos pos, Player player) {
        // Read live state, not a potentially stale interaction snapshot from another bite.
        BlockState live = level.getBlockState(pos);
        if (!live.is(this) || player.isSpectator() || !player.canEat(false)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        int bites = live.getValue(BITES);
        if (bites == SERVINGS - 1) level.removeBlock(pos, false);
        else level.setBlockAndUpdate(pos, live.setValue(BITES, bites + 1));
        player.getFoodData().eat(2, 0.6F);
        level.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.7F, 1.0F);
        level.gameEvent(player, GameEvent.EAT, pos);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                      BlockPos pos, Direction direction, BlockPos neighborPos,
                                      BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double cut = CUTS[state.getValue(BITES)];
        double height = platter ? 11 : 10;
        var roast = switch (state.getValue(FACING)) {
            case SOUTH -> box(1, 0, 1, 16 - cut, height, 15);
            case EAST -> box(1, 0, cut, 15, height, 15);
            case WEST -> box(1, 0, 1, 15, height, 16 - cut);
            default -> box(cut, 0, 1, 15, height, 15);
        };
        return platter ? Shapes.or(roast, box(0, 0, 0, 16, 1.5, 16)) : roast;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction side) {
        return 15 - 2 * state.getValue(BITES);
    }
}
