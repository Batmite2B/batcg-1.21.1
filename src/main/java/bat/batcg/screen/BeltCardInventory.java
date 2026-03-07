package bat.batcg.screen;

import bat.batcg.belt.BeltCards;
import bat.batcg.card.CardTier;
import bat.batcg.card.ModCardComponents;
import bat.batcg.item.PokemonCardItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class BeltCardInventory implements Inventory {

    private final ItemStack beltStack;
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(BeltCards.SLOTS, ItemStack.EMPTY);

    public BeltCardInventory(ItemStack beltStack) {
        this.beltStack = beltStack;

        for (int i = 0; i < BeltCards.SLOTS; i++) {
            BeltCards.SlotData d = BeltCards.get(beltStack, i);
            if (d == null || d.isEmpty()) {
                stacks.set(i, ItemStack.EMPTY);
            } else {
                ItemStack card = PokemonCardItem.createCard(d.pokemonId(), d.tier());
                bat.batcg.card.CardGradeData.setGrade(card, d.grade());
                stacks.set(i, card);
            }
        }
    }

    @Override public int size() { return stacks.size(); }

    @Override public boolean isEmpty() {
        for (ItemStack s : stacks) if (!s.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getStack(int slot) { return stacks.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack current = stacks.get(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;

        ItemStack taken = current.split(amount);
        if (!taken.isEmpty()) markDirty();
        return taken;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack out = stacks.get(slot);
        stacks.set(slot, ItemStack.EMPTY);
        markDirty();
        return out;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > 1) stack.setCount(1);
        markDirty();
    }

    @Override
    public void clear() {
        for (int i = 0; i < size(); i++) stacks.set(i, ItemStack.EMPTY);
        markDirty();
    }

    @Override
    public void markDirty() {
        // Si el belt no existe (por seguridad), NO borres nada.
        if (beltStack == null || beltStack.isEmpty()) return;

        for (int i = 0; i < BeltCards.SLOTS; i++) {
            ItemStack s = stacks.get(i);

            if (s.isEmpty()) {
                BeltCards.clear(beltStack, i);
                continue;
            }

            // Solo aceptamos cartas
            if (!(s.getItem() instanceof PokemonCardItem)) {
                BeltCards.clear(beltStack, i);
                continue;
            }

            // ✅ LEER DESDE DATA COMPONENTS (esto arregla tu bug)
            String id = s.getOrDefault(ModCardComponents.POKEMON_ID, "");
            String tierName = s.getOrDefault(ModCardComponents.CARD_TIER, "");

            if (id.isBlank() || tierName.isBlank()) {
                BeltCards.clear(beltStack, i);
                continue;
            }

            CardTier tier;
            try {
                tier = CardTier.valueOf(tierName);
            } catch (Exception e) {
                tier = null;
            }

            if (tier == null) {
                BeltCards.clear(beltStack, i);
            } else {
                int grade = bat.batcg.card.CardGradeData.getGrade(s);
                BeltCards.set(beltStack, i, id, tier, grade);
            }
        }
    }

    @Override public boolean canPlayerUse(PlayerEntity player) { return true; }
}
