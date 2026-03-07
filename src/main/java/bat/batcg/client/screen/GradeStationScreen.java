package bat.batcg.client.screen;

import bat.batcg.Batcg;
import bat.batcg.screen.GradeStationScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GradeStationScreen extends HandledScreen<GradeStationScreenHandler> {

    // ✅ Tu textura custom
    private static final Identifier BG = Identifier.of(Batcg.MOD_ID, "textures/gui/grade_station.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public GradeStationScreen(GradeStationScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // Botón donde lo tenías, luego lo movemos si quieres
        int btnX = this.x + 96;
        int btnY = this.y + 58;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("GRADE"), b -> {
            if (client != null && client.interactionManager != null) {
                client.interactionManager.clickButton(handler.syncId, GradeStationScreenHandler.BTN_GRADE);
            }
        }).dimensions(btnX, btnY, 56, 16).build());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // ✅ Dibuja 176x166 desde un PNG 256x256
        context.drawTexture(BG, x, y, 0, 0, backgroundWidth, backgroundHeight, TEX_W, TEX_H);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // si NO quieres título, borra estas dos líneas
        context.drawText(textRenderer, this.title, 8, 6, 0x404040, false);
        context.drawText(textRenderer, this.playerInventoryTitle, 8, this.playerInventoryTitleY, 0x404040, false);

        // ✅ Info útil (calculada desde el handler)
        GradeStationScreenHandler.Preview p = handler.preview();
        if (p != null && p.valid()) {

            int x0 = -80;   // ✅ más a la izquierda (prueba 2, 0 o incluso -2)
            int y0 = 56;

            context.drawText(textRenderer, Text.literal("Success: " + p.successText()), x0, y0,      0xC3C3C3, false);
            context.drawText(textRenderer, Text.literal("Break:   " + p.breakText()),   x0, y0 + 10, 0xC3C3C3, false);
            context.drawText(textRenderer, Text.literal("Gain:    " + p.gainText()),    x0, y0 + 20, 0xC3C3C3, false);
            context.drawText(textRenderer, Text.literal("Grade:   " + p.gradeText()),   x0, y0 + 30, 0xC3C3C3, false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}