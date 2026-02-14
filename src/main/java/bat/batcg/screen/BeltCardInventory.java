package bat.batcg.screen;

import bat.batcg.belt.BeltCards;
import bat.batcg.card.CardTier;
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
                stacks.set(i, PokemonCardItem.createCard(d.pokemonId(), d.tier()));
            }
        }
    }

    @Override public int size() { return stacks.size(); }

    @Override
    public boolean isEmpty() {
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
        for (int i = 0; i < BeltCards.SLOTS; i++) {
            ItemStack s = stacks.get(i);

            if (s.isEmpty()) {
                BeltCards.clear(beltStack, i);
                continue;
            }

            if (!(s.getItem() instanceof PokemonCardItem)) {
                BeltCards.clear(beltStack, i);
                continue;
            }

            String id = PokemonCardItem.getPokemonId(s);
            CardTier tier = PokemonCardItem.getTier(s);

            if (id == null || id.isBlank() || tier == null) {
                BeltCards.clear(beltStack, i);
            } else {
                BeltCards.set(beltStack, i, id, tier);
            }
        }
    }

    @Override public boolean canPlayerUse(PlayerEntity player) { return true; }
}
