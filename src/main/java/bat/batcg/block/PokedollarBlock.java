package bat.batcg.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import java.util.List;

public class PokedollarBlock extends Block {

    public static final IntProperty LAYERS = IntProperty.of("layers", 1, 16);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    // Ajusta si quieres más “tight”
    private static final double FROM_X = 5.0;
    private static final double TO_X   = 11.0;
    private static final double FROM_Z = 5.0;
    private static final double TO_Z   = 11.0;

    private static final VoxelShape[] SHAPES = new VoxelShape[17]; // 1..16

    static {
        for (int i = 1; i <= 16; i++) {
            SHAPES[i] = Block.createCuboidShape(FROM_X, 0.0, FROM_Z, TO_X, i, TO_Z);
        }
    }

    public PokedollarBlock(Settings settings) {
        super(settings);
        this.setDefaultState(
                this.getStateManager().getDefaultState()
                        .with(LAYERS, 1)
                        .with(FACING, Direction.NORTH)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LAYERS, FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        BlockState current = ctx.getWorld().getBlockState(pos);

        // Si ya hay una pila, solo sube layers y conserva la dirección original
        if (current.isOf(this)) {
            int layers = current.get(LAYERS);
            if (layers < 16) return current.with(LAYERS, layers + 1);
            return current;
        }

        // Dirección según como lo pones (que “mire” hacia ti)
        Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();
        return this.getDefaultState().with(FACING, facing);
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext ctx) {
        if (ctx.getStack().isOf(this.asItem())) {
            int layers = state.get(LAYERS);
            if (layers < 16) {
                return ctx.getSide() == Direction.UP || ctx.canReplaceExisting();
            }
        }
        return super.canReplace(state, ctx);
    }

    // Rotación/mirror para estructura y comandos
    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    // ✅ Drops = layers
    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        return List.of(new ItemStack(this.asItem(), state.get(LAYERS)));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(LAYERS)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(LAYERS)];
    }

    @Override
    public VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.empty();
    }
}
