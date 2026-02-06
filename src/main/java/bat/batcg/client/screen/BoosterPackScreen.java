package bat.batcg.client.screen;

import bat.batcg.card.CardTier;
import bat.batcg.item.PokemonCardItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BoosterPackScreen extends Screen {

    // Puedes cambiar esto por tus texturas
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

    public void applyRevealFromServer(int slot, String pokemonId, String tierName, int newMask) {
        if (slot < 0 || slot > 2) return;
        CardTier tier;
        try { tier = CardTier.valueOf(tierName); } catch (Exception e) { tier = CardTier.COMMON; }

        slots[slot].pokemonId = pokemonId;
        slots[slot].tier = tier;
        slots[slot].revealed = true;
        this.revealedMask = newMask;
    }

    @Override
    protected void init() {
        super.init();

        // si el servidor ya marcó reveladas (ej: reabres), las mostramos como back si no nos dio data aún
        for (int i = 0; i < 3; i++) {
            slots[i].revealed = ((revealedMask & (1 << i)) != 0);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);

        int cx = this.width / 2;
        int cy = this.height / 2;

        // BG (opcional)
        // ctx.drawTexture(BG, cx - 128, cy - 80, 0, 0, 256, 160, 256, 160);

        // Slots positions
        int w = 48, h = 64;
        int y = cy - 32;
        int x0 = cx - 80;
        int x1 = cx - 24;
        int x2 = cx + 32;

        drawCardSlot(ctx, x0, y, w, h, 0);
        drawCardSlot(ctx, x1, y, w, h, 1);
        drawCardSlot(ctx, x2, y, w, h, 2);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawCardSlot(DrawContext ctx, int x, int y, int w, int h, int slot) {
        SlotState s = slots[slot];

        if (!s.revealed) {
            // back
            ctx.drawTexture(CARD_BACK, x, y, 0, 0, w, h, w, h);
        } else {
            if (s.pokemonId != null && s.tier != null) {
                var stack = PokemonCardItem.createCard(s.pokemonId, s.tier);

                // render item (usa tu renderer ya hecho)
                ctx.getMatrices().push();
                ctx.getMatrices().translate(x, y, 200);
                ctx.drawItem(stack, 0, 0);
                ctx.drawItemInSlot(MinecraftClient.getInstance().textRenderer, stack, 0, 0);
                ctx.getMatrices().pop();
            } else {
                // Si está "revelado" pero aún no recibimos el id/tier del server, mostramos back de placeholder
                ctx.drawTexture(CARD_BACK, x, y, 0, 0, w, h, w, h);
            }
        }
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
                // si ya estaba revelada, no hace nada
                if ((revealedMask & (1 << i)) != 0) return true;

                // pedir al server revelar
                bat.batcg.network.ClientBoosterNetworking.requestReveal(handOrdinal, i);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static final class SlotState {
        boolean revealed;
        String pokemonId;
        CardTier tier;
    }
}
