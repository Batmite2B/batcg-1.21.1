package bat.batcg.client.screen;

import bat.batcg.card.CardTier;
import bat.batcg.card.ModBoosterComponents;
import bat.batcg.item.PokemonCardItem;
import bat.batcg.network.ClientBoosterNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class BoosterPackScreen extends Screen {

    private static final Identifier BG = Identifier.of("batcg", "textures/gui/booster_bg.png");
    private static final Identifier CARD_BACK = Identifier.of("batcg", "textures/gui/card_back.png");

    private final int handOrdinal;
    private int revealedMask;

    private final SlotState[] slots = new SlotState[] {
            new SlotState(), new SlotState(), new SlotState()
    };

    private static final int SLOT_W = 72;
    private static final int SLOT_H = 96;
    private static final int GAP = 12;

    private int startX;
    private int startY;

    public BoosterPackScreen(int handOrdinal, int revealedMask) {
        super(Text.literal("Booster Pack"));
        this.handOrdinal = handOrdinal;
        this.revealedMask = revealedMask;
    }

    public void applyRevealFromServer(int slot, String pokemonId, String tierName, int newMask) {
        if (slot < 0 || slot > 2) return;

        CardTier tier;
        try {
            tier = CardTier.valueOf(tierName);
        } catch (Exception ignored) {
            tier = CardTier.COMMON;
        }

        SlotState s = slots[slot];
        s.pokemonId = pokemonId;
        s.tier = tier;
        s.revealed = true;

        this.revealedMask = newMask;
    }

    @Override
    protected void init() {

        super.init();
        recalcLayout();

        // ✅ Al abrir, reconstruye slots revelados desde los components del booster.
        syncFromHeldBooster();
    }

    private void syncFromHeldBooster() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Hand hand = Hand.MAIN_HAND;
        Hand[] hands = Hand.values();
        if (handOrdinal >= 0 && handOrdinal < hands.length) {
            hand = hands[handOrdinal];
        }

        ItemStack booster = client.player.getStackInHand(hand);

        for (int i = 0; i < 3; i++) {
            SlotState s = slots[i];

            boolean bitRevealed = (revealedMask & (1 << i)) != 0;
            s.revealed = bitRevealed;

            // Reset
            s.pokemonId = null;
            s.tier = null;

            if (!bitRevealed) continue;

            String id = switch (i) {
                case 0 -> booster.get(ModBoosterComponents.BOOSTER_ID0);
                case 1 -> booster.get(ModBoosterComponents.BOOSTER_ID1);
                default -> booster.get(ModBoosterComponents.BOOSTER_ID2);
            };

            String tierName = switch (i) {
                case 0 -> booster.get(ModBoosterComponents.BOOSTER_TIER0);
                case 1 -> booster.get(ModBoosterComponents.BOOSTER_TIER1);
                default -> booster.get(ModBoosterComponents.BOOSTER_TIER2);
            };

            if (id == null || tierName == null) {
                // ✅ Slot revelado pero sin data -> lo dejaremos INVISIBLE (no back)
                continue;
            }

            try {
                s.pokemonId = id;
                s.tier = CardTier.valueOf(tierName);
            } catch (Exception ignored) {
                // Si algo está mal, también queda invisible
                s.pokemonId = null;
                s.tier = null;
            }
        }
    }

    private void recalcLayout() {
        int totalW = SLOT_W * 3 + GAP * 2;
        this.startX = (this.width - totalW) / 2;
        this.startY = (this.height - SLOT_H) / 2;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xB0000000);
        recalcLayout();

        for (int i = 0; i < 3; i++) {
            int x = startX + i * (SLOT_W + GAP);
            drawCardSlot(ctx, x, startY, SLOT_W, SLOT_H, i);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void drawCardSlot(DrawContext ctx, int x, int y, int w, int h, int slot) {
        SlotState s = slots[slot];

        boolean bitRevealed = (revealedMask & (1 << slot)) != 0;

        // ✅ Si NO está revelado -> back
        if (!bitRevealed) {
            // evita tile: textureWidth/Height = w/h
            ctx.drawTexture(CARD_BACK, x, y, 0, 0, w, h, w, h);
            return;
        }

        // ✅ Si está revelado pero NO tenemos data -> INVISIBLE (no back)
        if (s.pokemonId == null || s.tier == null) {
            return;
        }

        // --- FRONT (tu overscan) ---
        ItemStack stack = PokemonCardItem.createCard(s.pokemonId, s.tier);

        float baseX = w / 16.0f;
        float baseY = h / 16.0f;

        float overscanX = 1.30f; // tu valor perfecto
        float overscanY = 1.0f;

        float scaleX = baseX * overscanX;
        float scaleY = baseY * overscanY;

        float drawW = 16.0f * scaleX;
        float drawH = 16.0f * scaleY;

        float dx = x + (w - drawW) / 2.0f;
        float dy = y + (h - drawH) / 2.0f;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(dx, dy, 300.0f);
        ctx.getMatrices().scale(scaleX, scaleY, 1.0f);
        ctx.drawItem(stack, 0, 0);
        ctx.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        for (int i = 0; i < 3; i++) {
            int x = startX + i * (SLOT_W + GAP);
            int y = startY;

            if (inside(mouseX, mouseY, x, y, SLOT_W, SLOT_H)) {
                if ((revealedMask & (1 << i)) != 0) return true;
                ClientBoosterNetworking.requestReveal(handOrdinal, i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static final class SlotState {
        boolean revealed;
        String pokemonId;
        CardTier tier;
    }
}
