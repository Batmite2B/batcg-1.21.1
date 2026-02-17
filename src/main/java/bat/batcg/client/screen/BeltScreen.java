package bat.batcg.client.screen;

import bat.batcg.Batcg;
import bat.batcg.belt.BeltCards;
import bat.batcg.screen.BeltScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BeltScreen extends HandledScreen<BeltScreenHandler> {

    private static final Identifier TEXTURE =
            Identifier.of(Batcg.MOD_ID, "textures/gui/belt.png");

    public BeltScreen(BeltScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);

        // ✅ Hopper-like size
        this.backgroundWidth = 176;
        this.backgroundHeight = 133;

        this.titleX = 8;
        this.titleY = 6;

        // (Opcional) mueve el texto "Inventory" si lo usas
        // this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // ✅ Tu archivo es 256x256, pero dibujamos solo 176x133 desde (0,0)
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight, 256, 256);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);

        int filled = 0;
        for (int i = 0; i < BeltCards.SLOTS; i++) {
            ItemStack s = this.handler.getSlot(i).getStack();
            if (!s.isEmpty()) filled++;
        }

        Text t = Text.literal("Cards: " + filled + "/" + BeltCards.SLOTS);
        int textX = this.backgroundWidth - 8 - this.textRenderer.getWidth(t);
        int textY = 6;

        context.drawText(this.textRenderer, t, textX, textY, 0xFFE6C76B, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
