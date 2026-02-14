package bat.batcg.item;

import bat.batcg.block.PokedollarBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.server.network.ServerPlayerEntity;


public class PokedollarBlockItem extends BlockItem {

    public PokedollarBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public ActionResult place(ItemPlacementContext ctx) {

        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();

        // Shift = sneak
        boolean sneak = ctx.getPlayer() != null && ctx.getPlayer().isSneaking();

        // =========================
        // NORMAL PLACE (1 click)
        // =========================
        if (!sneak) {
            BlockState before = world.getBlockState(pos);
            int beforeLayers = before.isOf(this.getBlock()) ? before.get(PokedollarBlock.LAYERS) : 0;

            ActionResult res = super.place(ctx);

            if (res.isAccepted() && !world.isClient) {
                BlockState after = world.getBlockState(pos);
                if (after.isOf(this.getBlock())) {
                    int afterLayers = after.get(PokedollarBlock.LAYERS);

                    int added = Math.max(1, afterLayers - beforeLayers);
                    boolean becameFull = (beforeLayers < 16 && afterLayers == 16);

                    playCoinSound(world, pos, becameFull);
                    spawnCoinParticles(world, pos, added, afterLayers, becameFull);
                }
            }
            return res;
        }

        // =========================
        // SHIFT-CLICK: fill up to 16
        // =========================
        BlockState current = world.getBlockState(pos);

        // Si no es reemplazable y no es nuestra pila, deja vanilla
        if (!current.isOf(this.getBlock()) && !current.canReplace(ctx)) {
            return super.place(ctx);
        }

        int currentLayers = current.isOf(this.getBlock()) ? current.get(PokedollarBlock.LAYERS) : 0;
        int space = 16 - currentLayers;
        if (space <= 0) return ActionResult.FAIL;

        boolean creative = ctx.getPlayer() != null && ctx.getPlayer().getAbilities().creativeMode;
        int stackCount = ctx.getStack().getCount();

        int add = creative ? space : Math.min(space, Math.min(16, stackCount));
        if (add <= 0) return ActionResult.FAIL;

        BlockState newState;
        if (current.isOf(this.getBlock())) {
            // Mantén facing y sube layers
            newState = current.with(PokedollarBlock.LAYERS, currentLayers + add);
        } else {
            // Bloque nuevo: layers = add y facing hacia el jugador
            Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();
            newState = this.getBlock().getDefaultState()
                    .with(PokedollarBlock.LAYERS, add)
                    .with(PokedollarBlock.FACING, facing);
        }

        if (!newState.canPlaceAt(world, pos)) return ActionResult.FAIL;

        if (!world.isClient) {
            world.setBlockState(pos, newState, Block.NOTIFY_ALL);
            // ✅ Dispara el trigger vanilla para que funcione el advancement (placed_block)
            if (ctx.getPlayer() instanceof ServerPlayerEntity sp) {
                ItemStack used = ctx.getStack().copy();
                used.setCount(1); // el criterio espera el item usado, no importa el count real
                Criteria.PLACED_BLOCK.trigger(sp, pos, used);
            }


            if (!creative) ctx.getStack().decrement(add);

            int finalLayers = newState.get(PokedollarBlock.LAYERS);
            boolean becameFull = (currentLayers < 16 && finalLayers == 16);

            playCoinSound(world, pos, becameFull);
            spawnCoinParticles(world, pos, add, finalLayers, becameFull);
        }

        return ActionResult.success(world.isClient);
    }

    // ✅ Mismo sonido siempre + extra cuando llegas a 16
    private void playCoinSound(World world, BlockPos pos, boolean becameFullStack) {
        // Sonido de “moneda” que te gustó
        world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.BLOCKS, 0.8f, 1.15f);

        // “OMG” al completar la pila
        if (becameFullStack) {
            world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP,
                    SoundCategory.BLOCKS, 0.9f, 1.0f);
        }
    }

    // ✅ Partículas siempre visibles + burst brutal al llegar a 16
    private void spawnCoinParticles(World world, BlockPos pos, int added, int finalLayers, boolean becameFullStack) {
        if (!(world instanceof ServerWorld sw)) return;

        // altura del tope según layers (1..16)
        double topY = pos.getY() + (finalLayers / 16.0) + 0.05;
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;

        ItemStackParticleEffect coinFx = new ItemStackParticleEffect(
                ParticleTypes.ITEM,
                new ItemStack(this)
        );

        // Siempre visibles: más cantidad, salen desde arriba
        int coinCount = 10 + (added * 6);
        double spread = 0.22;

        sw.spawnParticles(coinFx, x, topY, z, coinCount, spread, 0.06, spread, 0.04);


        // OMG pila completa
        if (becameFullStack) {

            sw.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, x, topY + 0.05, z,
                    25, 0.30, 0.18, 0.30, 0.05);


        }
    }
}
