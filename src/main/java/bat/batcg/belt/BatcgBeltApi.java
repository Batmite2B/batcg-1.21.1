package bat.batcg.belt;

import bat.batcg.item.CardBeltItem;
import bat.batcg.screen.BeltScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public final class BatcgBeltApi {

    private BatcgBeltApi() {}

    // =========================
    // Equipped slot access
    // =========================

    /** Returns the equipped belt stack (our custom slot). */
    public static ItemStack getEquippedBelt(PlayerEntity player) {
        if (!(player instanceof BeltSlotAccess access)) return ItemStack.EMPTY;

        SimpleInventory inv = access.batcg$getBeltSlotInventory();
        if (inv == null || inv.size() < 1) return ItemStack.EMPTY;

        ItemStack stack = inv.getStack(0);
        return (stack.getItem() instanceof CardBeltItem) ? stack : ItemStack.EMPTY;
    }

    public static boolean hasEquippedBelt(PlayerEntity player) {
        return !getEquippedBelt(player).isEmpty();
    }

    /**
     * Para pantallas:
     * - Si el belt está en mano, usa ese (Shift+click).
     * - Si no, usa el equipado (K).
     */
    public static ItemStack findBeltForScreen(PlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        if (main.getItem() instanceof CardBeltItem) return main;

        ItemStack off = player.getOffHandStack();
        if (off.getItem() instanceof CardBeltItem) return off;

        return getEquippedBelt(player);
    }

    // =========================
    // Equip
    // =========================

    /**
     * CLICK DERECHO SIN SHIFT:
     * equipa el belt desde la mano al slot custom.
     * Si ya hay uno equipado, lo intercambia.
     */
    public static boolean equipFromHand(PlayerEntity player, Hand hand) {
        if (!(player instanceof BeltSlotAccess access)) return false;

        ItemStack handStack = player.getStackInHand(hand);
        if (!(handStack.getItem() instanceof CardBeltItem)) return false;

        SimpleInventory beltSlot = access.batcg$getBeltSlotInventory();
        if (beltSlot == null || beltSlot.size() < 1) return false;

        ItemStack currentlyEquipped = beltSlot.getStack(0);

        // Colocar 1 belt en el slot
        ItemStack toEquip = handStack.copy();
        toEquip.setCount(1);
        beltSlot.setStack(0, toEquip);
        beltSlot.markDirty();

        // Swap / consumo
        if (handStack.getCount() == 1) {
            // mano queda con el que estaba equipado (o empty)
            player.setStackInHand(hand, currentlyEquipped);
        } else {
            // si por algún motivo estaba stackeado, consume 1
            handStack.decrement(1);

            // devolver el que estaba equipado al inventario o dropear
            if (!currentlyEquipped.isEmpty()) {
                if (!player.getInventory().insertStack(currentlyEquipped)) {
                    player.dropItem(currentlyEquipped, false);
                }
            }
        }

        return true;
    }

    // =========================
    // Open GUI
    // =========================

    /**
     * Abre la GUI del belt:
     * - Shift+click: abrirá el belt en mano (porque findBeltForScreen lo prioriza)
     * - K: abrirá el equipado
     */
    public static void openBeltScreen(ServerPlayerEntity player) {
        ItemStack belt = findBeltForScreen(player);
        if (belt.isEmpty()) return;

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new BeltScreenHandler(syncId, inv),
                Text.literal("Belt")
        ));
    }

    /**
     * Solo abre si está equipado (para el keybind K).
     * Esto es lo que llama tu receiver en BatcgPackets.
     */
    public static void openEquippedBeltScreen(ServerPlayerEntity player) {
        if (getEquippedBelt(player).isEmpty()) return;
        openBeltScreen(player);
    }
}
