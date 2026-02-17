package bat.batcg.item;

import bat.batcg.belt.BatcgBeltApi;
import bat.batcg.card.CardTier;
import bat.batcg.card.ModCardComponents;
import bat.batcg.screen.BeltCardInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

public class CardBeltItem extends Item {

    public CardBeltItem(Settings settings) {
        super(settings);
    }

    // =========================
    // Tooltip
    // =========================
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        // No dependemos de BeltCards.SLOTS (en tu build actual no existe)
        BeltCardInventory inv = new BeltCardInventory(stack);
        int total = inv.size();
        int filled = countFilled(inv);

        // Count
        tooltip.add(
                Text.literal("Cards: ").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal(filled + "/" + total).formatted(Formatting.GOLD))
        );

        // Instructions (EN)
        tooltip.add(Text.literal("Right-click: Equip").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Shift + Right-click: Open belt").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Press K while equipped: Open belt").formatted(Formatting.GRAY));

        // Shift details
        if (!isShiftDown()) {
            tooltip.add(Text.literal("Hold Shift to view contents").formatted(Formatting.DARK_GRAY));
            return;
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.literal("Contents:").formatted(Formatting.YELLOW));

        if (filled == 0) {
            tooltip.add(Text.literal("(empty)").formatted(Formatting.DARK_GRAY));
            return;
        }

        for (int i = 0; i < total; i++) {
            ItemStack card = inv.getStack(i);
            if (card.isEmpty()) continue;

            String tierName = card.get(ModCardComponents.CARD_TIER);
            CardTier tier = safeTier(tierName);

            tooltip.add(
                    Text.literal((i + 1) + ". ").formatted(Formatting.DARK_GRAY)
                            .append(card.getName().copy().formatted(Formatting.WHITE))
                            .append(Text.literal(" — ").formatted(Formatting.DARK_GRAY))
                            .append(Text.literal(tier.getDisplayName()).formatted(tier.getColor()))
            );
        }
    }

    private static int countFilled(BeltCardInventory inv) {
        int c = 0;
        for (int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) c++;
        }
        return c;
    }

    private static CardTier safeTier(String tierName) {
        if (tierName == null) return CardTier.COMMON;
        try {
            return CardTier.valueOf(tierName.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return CardTier.COMMON;
        }
    }

    // Safe Shift detection (no client-only imports in common code)
    private static Method SHIFT_DOWN;

    private static boolean isShiftDown() {
        try {
            if (SHIFT_DOWN == null) {
                Class<?> screen = Class.forName("net.minecraft.client.gui.screen.Screen");
                SHIFT_DOWN = screen.getMethod("hasShiftDown");
            }
            return (boolean) SHIFT_DOWN.invoke(null);
        } catch (Throwable ignored) {
            return false;
        }
    }

    // =========================
    // Use logic
    // =========================
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // SHIFT + Right Click => SOLO abrir GUI
        if (user.isSneaking()) {
            if (!world.isClient && user instanceof ServerPlayerEntity sp) {
                BatcgBeltApi.openBeltScreen(sp);
            }
            return TypedActionResult.success(stack, world.isClient);
        }

        // Click normal => EQUIPAR
        if (!world.isClient) {
            BatcgBeltApi.equipFromHand(user, hand);
        }
        return TypedActionResult.success(stack, world.isClient);
    }
}
