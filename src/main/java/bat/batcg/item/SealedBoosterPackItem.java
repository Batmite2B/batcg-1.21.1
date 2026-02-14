package bat.batcg.item;

import bat.batcg.item.ModItems;
import bat.batcg.network.payload.OpenBoosterS2CPayload;
import bat.batcg.card.ModBoosterComponents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Booster sellado (stackeable). No guarda estado.
 * Al usarlo:
 * - Si count == 1: lo reemplaza en la mano por un booster abierto y abre la UI.
 * - Si count > 1: requiere OFFHAND vacía; consume 1 sellado, pone el abierto en OFFHAND y abre la UI.
 */
public class SealedBoosterPackItem extends Item {

    public SealedBoosterPackItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack sealedStack = user.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.success(sealedStack);
        }

        if (!(user instanceof ServerPlayerEntity sp)) {
            return TypedActionResult.success(sealedStack);
        }

        // ✅ Seguridad: el sellado NO debería tener data; si la tuviera por legado, la limpiamos.
        BoosterPackItem.clearBoosterData(sealedStack);

        // Caso 1: stack de 1 -> reemplaza en la misma mano por el OPENED
        if (sealedStack.getCount() == 1) {
            ItemStack opened = new ItemStack(ModItems.BOOSTER_PACK_OPENED);

            BoosterPackItem.ensureSeed(opened, world.random.nextLong());
            opened.set(ModBoosterComponents.BOOSTER_REVEALED, 0);

            sp.setStackInHand(hand, opened);

            ServerPlayNetworking.send(sp, new OpenBoosterS2CPayload(hand.ordinal(), 0));
            return TypedActionResult.success(opened, false);
        }

        // Caso 2: stack > 1 -> NO movemos el stack principal; ponemos el OPENED en OFFHAND
        if (!sp.getOffHandStack().isEmpty()) {
            sp.sendMessage(Text.literal("Empty your offhand to open a booster from a stack."), true);
            return TypedActionResult.fail(sealedStack);
        }

        // Consumimos 1 sellado
        sealedStack.decrement(1);

        // Creamos el booster abierto en OFFHAND
        ItemStack opened = new ItemStack(ModItems.BOOSTER_PACK_OPENED);
        BoosterPackItem.ensureSeed(opened, world.random.nextLong());
        opened.set(ModBoosterComponents.BOOSTER_REVEALED, 0);

        sp.setStackInHand(Hand.OFF_HAND, opened);

        // Abrimos la UI apuntando a OFFHAND
        ServerPlayNetworking.send(sp, new OpenBoosterS2CPayload(Hand.OFF_HAND.ordinal(), 0));

        return TypedActionResult.success(sealedStack, false);
    }
}
