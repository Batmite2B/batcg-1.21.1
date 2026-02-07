package bat.batcg.client.screen;

import bat.batcg.card.CardTier;
import bat.batcg.item.PokemonCardItem;
import bat.batcg.network.ClientBoosterNetworking;
import net.minecraft.client.MinecraftClient;
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

        // Si el server ya marcó reveladas, reflejarlo visualmente
        for (int i = 0; i < 3; i++) {
            slots[i].revealed = ((revealedMask & (1 << i)) != 0);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // ✅ 1.21.1 requiere 4 args
        this.renderBackground(ctx, mouseX, mouseY, delta);

        int cx = this.width / 2;
        int cy = this.height / 2;

        // (Opcional) dibujar BG si tienes textura real 256x160
        // ctx.drawTexture(BG, cx - 128, cy - 80, 0, 0, 256, 160, 256, 160);

        // Slots
        int slotW = 48, slotH = 64;
        int y = cy - 32;
        int x0 = cx - 80;
        int x1 = cx - 24;
        int x2 = cx + 32;

        drawCardSlot(ctx, x0, y, slotW, slotH, 0);
        drawCardSlot(ctx, x1, y, slotW, slotH, 1);
        drawCardSlot(ctx, x2, y, slotW, slotH, 2);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawCardSlot(DrawContext ctx, int x, int y, int w, int h, int slot) {
        SlotState s = slots[slot];

        if (!s.revealed) {
            // back
            ctx.drawTexture(CARD_BACK, x, y, 0, 0, w, h, w, h);
            return;
        }

        // Revelado pero aún no recibimos data -> back placeholder
        if (s.pokemonId == null || s.tier == null) {
            ctx.drawTexture(CARD_BACK, x, y, 0, 0, w, h, w, h);
            return;
        }

        // Crear stack de la carta real
        ItemStack stack = PokemonCardItem.createCard(s.pokemonId, s.tier);

        // ✅ Render del item centrado dentro del “slot”
        // drawItem() renderiza el item como icono GUI (16x16). Lo escalamos para que llene 48x64.
        float scale = 3.0f; // 16*3 = 48 exacto de ancho
        int drawX = x;
        int drawY = y + (h - (int)(16 * scale)) / 2; // centra vertical (64 - 48)/2 = 8

        ctx.getMatrices().push();
        ctx.getMatrices().translate(drawX, drawY, 200.0f);
        ctx.getMatrices().scale(scale, scale, 1.0f);

        // Render del ítem (NO uses drawItemInSlot aquí)
        ctx.drawItem(stack, 0, 0);

        ctx.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int cx = this.width / 2;
        int cy = this.height / 2;

        int w = 48, h = 64;
        int y = cy - 32;
        int[] xs = new int[] { cx - 80, cx - 24, cx + 32 };

        for (int i = 0; i < 3; i++) {
            if (inside(mouseX, mouseY, xs[i], y, w, h)) {

                // si ya estaba revelada, no pedir de nuevo
                if ((revealedMask & (1 << i)) != 0) return true;

                // pedir al server revelar
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
