package bat.batcg.client.screen;

import bat.batcg.Batcg;
import bat.batcg.screen.CardBeltScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CardBeltScreen extends HandledScreen<CardBeltScreenHandler> {

    // Vanilla
    private static final Identifier GAMEMODE =
            Identifier.of("minecraft", "textures/gui/container/gamemode_switcher.png");

    // Tu slot
    private static final Identifier SLOT =
            Identifier.of(Batcg.MOD_ID, "textures/gui/card_belt_slot.png");

    public CardBeltScreen(CardBeltScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 220; // ✅ deja arriba la textura 128 y abajo el inventario
        this.titleX = 8;
        this.titleY = 6;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;

        // --- gamemode_switcher INVERTIDO (boca abajo) ---
        context.getMatrices().push();
        // dibujamos la textura “al revés” con flip X+Y (rotación 180)
        context.getMatrices().translate(x + 24 + 128, y + 128, 0);
        context.getMatrices().scale(-1f, -1f, 1f);
        context.drawTexture(GAMEMODE, 0, 0, 0, 0, 128, 128, 128, 128);
        context.getMatrices().pop();

        // --- slots dibujados sobre la barra blanca ---
        int slotStartX = x + 39;
        int slotY = y + 109;
        for (int i = 0; i < 5; i++) {
            context.drawTexture(SLOT, slotStartX + i * 20, slotY, 0, 0, 18, 18, 18, 18);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0xFFFFFF, false);
    }
}
