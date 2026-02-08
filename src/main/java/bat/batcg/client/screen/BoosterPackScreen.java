package bat.batcg.client.screen;

import bat.batcg.card.CardTier;
import bat.batcg.item.PokemonCardItem;
import bat.batcg.network.ClientBoosterNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BoosterPackScreen extends Screen {

    // Texturas (opcionales)
    private static final Identifier BG = Identifier.of("batcg", "textures/gui/booster_bg.png");
    private static final Identifier CARD_BACK = Identifier.of("batcg", "textures/gui/card_back.png");

    private final int handOrdinal;
    private int revealedMask;

    private final SlotState[] slots = new SlotState[] {
            new SlotState(), new SlotState(), new SlotState()
    };

    // ✅ UI settings (ajusta a gusto)
    private static final int SLOT_W = 72;
    private static final int SLOT_H = 96;
    private static final int GAP = 12;

    // Guardamos posiciones para clicks
    private int startX;
    private int startY;

    public BoosterPackScreen(int handOrdinal, int revealedMask) {
        super(Text.literal("Booster Pack"));
        this.handOrdinal = handOrdinal;
        this.revealedMask = revealedMask;
    }

    /**
     * Llamado desde el networking cuando el server confirma reveal.
     * newMask viene del server para evitar double-reveals.
     */
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

        for (int i = 0; i < 3; i++) {
            slots[i].revealed = ((revealedMask & (1 << i)) != 0);
        }

        recalcLayout();
    }

    private void recalcLayout() {
        int totalW = SLOT_W * 3 + GAP * 2;
        this.startX = (this.width - totalW) / 2;
        this.startY = (this.height - SLOT_H) / 2;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Fondo SIN blur (solo oscurece)
        ctx.fill(0, 0, this.width, this.height, 0xB0000000);

        // Si la ventana cambia, recalcular (seguro)
        recalcLayout();

        // (Opcional) si luego quieres un panel BG 256x160 centrado:
        // int cx = this.width / 2;
        // int cy = this.height / 2;
        // ctx.drawTexture(BG, cx - 128, cy - 80, 0, 0, 256, 160, 256, 160);

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

        // No revelado → back
        if (!s.revealed) {
            ctx.drawTexture(CARD_BACK, x, y, 0, 0, w, h, w, h);
            return;
        }

        // Revelado pero sin data aún → back placeholder
        if (s.pokemonId == null || s.tier == null) {
            ctx.drawTexture(CARD_BACK, x, y, 0, 0, w, h, w, h);
            return;
        }

        // Carta real
        ItemStack stack = PokemonCardItem.createCard(s.pokemonId, s.tier);

        // ✅ Escalado NO uniforme para que el render 16x16 llene w x h
        float scale = Math.min(w, h) / 16.0f; // escala uniforme

        float drawW = 16.0f * scale;
        float drawH = 16.0f * scale;

        float dx = x + (w - drawW) / 2.0f;
        float dy = y + (h - drawH) / 2.0f;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(dx, dy, 300.0f);
        ctx.getMatrices().scale(scale, scale, 1.0f);
        ctx.drawItem(stack, 0, 0);
        ctx.getMatrices().pop();



    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Hitboxes basados en el layout real (mismos tamaños y posiciones)
        for (int i = 0; i < 3; i++) {
            int x = startX + i * (SLOT_W + GAP);
            int y = startY;

            if (inside(mouseX, mouseY, x, y, SLOT_W, SLOT_H)) {
                if ((revealedMask & (1 << i)) != 0) return true; // ya revelada
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
