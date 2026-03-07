package bat.batcg.screen;

import bat.batcg.card.CardGradeData;
import bat.batcg.item.ModItems;
import bat.batcg.item.PokemonCardItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.Map;

public class GradeStationScreenHandler extends ScreenHandler {

    public static final int BTN_GRADE = 0;
    private final Inventory inv;

    // CLIENT
    public GradeStationScreenHandler(int syncId, PlayerInventory playerInv) {
        this(syncId, playerInv, new SimpleInventory(3));
    }

    // SERVER
    public GradeStationScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv) {
        super(ModScreenHandlers.GRADE_STATION, syncId);
        this.inv = inv;

        // card slot
        this.addSlot(new Slot(inv, 0, 44, 36) {
            @Override public boolean canInsert(ItemStack stack) { return stack.isOf(ModItems.POKEMONCARD); }
            @Override public int getMaxItemCount() { return 1; }
        });

        // catalyst slot
        this.addSlot(new Slot(inv, 1, 80, 36) {
            @Override public boolean canInsert(ItemStack stack) { return CATALYSTS.containsKey(stack.getItem()); }
        });

        // output slot
        this.addSlot(new Slot(inv, 2, 116, 36) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
        });

        // player inv
        int m, l;
        for (m = 0; m < 3; ++m) {
            for (l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInv, l + m * 9 + 9, 8 + l * 18, 84 + m * 18));
            }
        }
        for (m = 0; m < 9; ++m) {
            this.addSlot(new Slot(playerInv, m, 8 + m * 18, 142));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inv.canPlayerUse(player);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id != BTN_GRADE) return false;
        if (player.getWorld().isClient) return true;

        ItemStack card = inv.getStack(0);
        ItemStack catalyst = inv.getStack(1);
        ItemStack out = inv.getStack(2);

        if (!out.isEmpty()) {
            player.sendMessage(Text.literal("Take the result first."), true);
            return true;
        }
        if (card.isEmpty() || !card.isOf(ModItems.POKEMONCARD)) {
            player.sendMessage(Text.literal("Insert a card."), true);
            return true;
        }
        if (catalyst.isEmpty() || !CATALYSTS.containsKey(catalyst.getItem())) {
            player.sendMessage(Text.literal("Insert a catalyst."), true);
            return true;
        }

        int currentGrade = CardGradeData.getGrade(card);
        if (currentGrade >= 4) {
            player.sendMessage(Text.literal("This card is already Grade S."), true);
            return true;
        }

        Item ore = catalyst.getItem();
        CatalystInfo info = CATALYSTS.get(ore);

        // Consume catalyst always
        catalyst.decrement(1);

        // Success chance
        double success = (currentGrade == 3) ? successAtoS(ore) : info.success;

        // Optional tier penalty (harder for rare cards)
        var tier = PokemonCardItem.getTier(card);
        if (tier != null) {
            switch (tier) {
                case EPIC -> success -= 0.05;
                case LEGENDARY -> success -= 0.08;
                case SHINY -> success -= 0.10;
            }
        }
        success = clamp(success, 0.05, 1.0);

        boolean ok = player.getRandom().nextDouble() < success;

        // Break chance scales with grade, and extra danger on A->S
        double breakChance = info.breakOnFail + (currentGrade * 0.05) + (currentGrade == 3 ? 0.20 : 0.0);
        breakChance = clamp(breakChance, 0.0, 0.85);

        if (ok) {
            int gain = rollGain(player, ore, currentGrade);

            // Skips allowed only up to A (3). S only possible from A->S.
            int cap = (currentGrade < 3) ? 3 : 4;
            int newGrade = Math.min(cap, currentGrade + gain);

            ItemStack result = card.copy();
            result.setCount(1);
            CardGradeData.setGrade(result, newGrade);
            CardGradeData.setAttempts(result, 0);

            inv.setStack(0, ItemStack.EMPTY);
            inv.setStack(2, result);

            if (player instanceof ServerPlayerEntity sp) {
                sp.getServerWorld().playSound(null, sp.getBlockPos(),
                        SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 0.85f, 1.25f);
            }

            player.sendMessage(Text.literal("Success! Grade: " + CardGradeData.label(newGrade)), true);
            return true;
        }

        // FAIL
        int attempts = CardGradeData.getAttempts(card) + 1;
        boolean broke = player.getRandom().nextDouble() < breakChance;

        if (broke) {
            inv.setStack(0, ItemStack.EMPTY);
            player.sendMessage(Text.literal("Grading failed… the card was destroyed!"), true);

            if (player instanceof ServerPlayerEntity sp) {
                sp.getServerWorld().playSound(null, sp.getBlockPos(),
                        SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 0.9f, 0.8f);
            }
            return true;
        }

        ItemStack result = card.copy();
        result.setCount(1);
        CardGradeData.setGrade(result, currentGrade);
        CardGradeData.setAttempts(result, attempts);

        inv.setStack(0, ItemStack.EMPTY);
        inv.setStack(2, result);

        player.sendMessage(Text.literal("Grading failed."), true);
        if (player instanceof ServerPlayerEntity sp) {
            sp.getServerWorld().playSound(null, sp.getBlockPos(),
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.6f, 1.2f);
        }
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;

        ItemStack original = slot.getStack();
        newStack = original.copy();

        if (slotIndex < 3) {
            if (!this.insertItem(original, 3, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (original.isOf(ModItems.POKEMONCARD)) {
                if (!this.insertItem(original, 0, 1, false)) return ItemStack.EMPTY;
            } else if (CATALYSTS.containsKey(original.getItem())) {
                if (!this.insertItem(original, 1, 2, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();

        if (original.getCount() == newStack.getCount()) return ItemStack.EMPTY;
        slot.onTakeItem(player, original);
        return newStack;
    }

    // --- Gain rolls ---
    private static int rollGain(PlayerEntity player, Item ore, int currentGrade) {
        if (currentGrade >= 3) return 1; // A -> S only +1

        double r = player.getRandom().nextDouble();

        if (ore == Items.IRON_INGOT) return 1;
        if (ore == Items.GOLD_INGOT) return (r < 0.20) ? 2 : 1;

        if (ore == Items.DIAMOND) {
            if (r < 0.05) return 3;
            if (r < 0.40) return 2;
            return 1;
        }

        if (ore == Items.EMERALD) {
            if (r < 0.15) return 3;
            if (r < 0.55) return 2;
            return 1;
        }

        if (ore == Items.NETHERITE_INGOT) {
            return (r < 0.55) ? 2 : 3; // None->B or None->A
        }

        return 1;
    }

    // --- A -> S success table (SOFT): Netherite 60% ---
    private static double successAtoS(Item ore) {
        if (ore == Items.IRON_INGOT) return 0.12;
        if (ore == Items.GOLD_INGOT) return 0.18;
        if (ore == Items.DIAMOND) return 0.28;
        if (ore == Items.EMERALD) return 0.40;
        if (ore == Items.NETHERITE_INGOT) return 0.60;
        return 0.10;
    }

    // --- catalyst table ---
    private record CatalystInfo(double success, double breakOnFail) {}
    private static final Map<Item, CatalystInfo> CATALYSTS = Map.of(
            Items.IRON_INGOT, new CatalystInfo(0.50, 0.10),
            Items.GOLD_INGOT, new CatalystInfo(0.60, 0.10),
            Items.DIAMOND, new CatalystInfo(0.70, 0.08),
            Items.EMERALD, new CatalystInfo(0.75, 0.08),
            Items.NETHERITE_INGOT, new CatalystInfo(1.00, 0.00)
    );

    private static double clamp(double v, double a, double b) {
        return Math.max(a, Math.min(b, v));
    }

    // ---------------- UI Preview (client-side text) ----------------

    public Preview preview() {
        ItemStack card = inv.getStack(0);
        ItemStack catalyst = inv.getStack(1);
        if (card.isEmpty() || catalyst.isEmpty()) return Preview.empty();

        Item ore = catalyst.getItem();
        CatalystInfo info = CATALYSTS.get(ore);
        if (info == null) return Preview.empty();

        int grade = CardGradeData.getGrade(card);

        double success = (grade == 3) ? successAtoS(ore) : info.success;

        var tier = PokemonCardItem.getTier(card);
        if (tier != null) {
            switch (tier) {
                case EPIC -> success -= 0.05;
                case LEGENDARY -> success -= 0.08;
                case SHINY -> success -= 0.10;
            }
        }
        success = clamp(success, 0.05, 1.0);

        double breakChance = info.breakOnFail + (grade * 0.05) + (grade == 3 ? 0.20 : 0.0);
        breakChance = clamp(breakChance, 0.0, 0.85);

        int gainMin, gainMax;
        if (grade >= 3) {
            gainMin = gainMax = 1;
        } else if (ore == Items.IRON_INGOT) {
            gainMin = gainMax = 1;
        } else if (ore == Items.GOLD_INGOT) {
            gainMin = 1; gainMax = 2;
        } else if (ore == Items.DIAMOND) {
            gainMin = 1; gainMax = 3;
        } else if (ore == Items.EMERALD) {
            gainMin = 1; gainMax = 3;
        } else if (ore == Items.NETHERITE_INGOT) {
            gainMin = 2; gainMax = 3;
        } else {
            gainMin = gainMax = 1;
        }

        int cap = (grade < 3) ? 3 : 4;
        int maxPossible = Math.max(0, cap - grade);
        gainMin = Math.min(gainMin, maxPossible);
        gainMax = Math.min(gainMax, maxPossible);

        return new Preview(grade, success, breakChance, gainMin, gainMax);
    }

    public record Preview(int grade, double success, double breakChance, int gainMin, int gainMax) {

        public static Preview empty() {
            return new Preview(0, 0, 0, 0, 0);
        }

        public boolean valid() {
            return success > 0 && gainMax > 0;
        }

        public String successText() {
            return String.format(Locale.ROOT, "%.0f%%", success * 100.0);
        }

        public String breakText() {
            return String.format(Locale.ROOT, "%.0f%%", breakChance * 100.0);
        }

        public String gainText() {
            if (gainMin == gainMax) return "+" + gainMin;
            return "+" + gainMin + " .. +" + gainMax;
        }

        public String gradeText() {
            return CardGradeData.label(grade);
        }
    }
}