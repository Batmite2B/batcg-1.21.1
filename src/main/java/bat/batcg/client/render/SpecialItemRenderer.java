package bat.batcg.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Renderer base para dibujar modelos BAKED sin llamar ItemRenderer.renderItem (evita recursión).
 */
public abstract class SpecialItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {

    protected final void renderModel(Identifier modelId,
                                     ItemStack stack,
                                     ModelTransformationMode mode,
                                     boolean leftHanded,
                                     MatrixStack matrices,
                                     VertexConsumerProvider vertexConsumers,
                                     int light,
                                     int overlay) {
        renderModel(modelId, stack, mode, leftHanded, matrices, vertexConsumers, light, overlay, () -> {});
    }

    protected final void renderModel(Identifier modelId,
                                     ItemStack stack,
                                     ModelTransformationMode mode,
                                     boolean leftHanded,
                                     MatrixStack matrices,
                                     VertexConsumerProvider vertexConsumers,
                                     int light,
                                     int overlay,
                                     Runnable afterTransformBeforeDraw) {

        MinecraftClient client = MinecraftClient.getInstance();
        BakedModel model = client.getBakedModelManager().getModel(modelId);

        matrices.push();

        if (mode == ModelTransformationMode.GUI && !model.isSideLit()) {
            DiffuseLighting.disableGuiDepthLighting();
        }

        // Aplicar transforms del modelo para este mode (como vanilla)
        matrices.translate(0.5F, 0.5F, 0.5F);
        model.getTransformation().getTransformation(mode).apply(leftHanded, matrices);
        matrices.translate(-0.5F, -0.5F, -0.5F);

        afterTransformBeforeDraw.run();

        renderBakedModel(model, stack, matrices, vertexConsumers, light, overlay);

        if (mode == ModelTransformationMode.GUI && !model.isSideLit()) {
            DiffuseLighting.enableGuiDepthLighting();
        }

        matrices.pop();
    }

    private static void renderBakedModel(BakedModel model,
                                         ItemStack stack,
                                         MatrixStack matrices,
                                         VertexConsumerProvider vertexConsumers,
                                         int light,
                                         int overlay) {

        // Para items planos usamos el layer de item normal
        RenderLayer layer = RenderLayers.getItemLayer(stack, true);

        VertexConsumer vc = ItemRenderer.getDirectItemGlintConsumer(
                vertexConsumers, layer, true, stack.hasGlint()
        );

        Random random = Random.create();

        for (Direction dir : Direction.values()) {
            random.setSeed(42L);
            drawQuads(matrices, vc, model.getQuads(null, dir, random), light, overlay);
        }

        random.setSeed(42L);
        drawQuads(matrices, vc, model.getQuads(null, null, random), light, overlay);
    }

    private static void drawQuads(MatrixStack matrices,
                                  VertexConsumer vertices,
                                  List<BakedQuad> quads,
                                  int light,
                                  int overlay) {

        MatrixStack.Entry entry = matrices.peek();

        for (BakedQuad quad : quads) {
            // ✅ Sin tint: blanco sólido (ideal para cartas)
            vertices.quad(entry, quad, 1f, 1f, 1f, 1f, light, overlay);
        }
    }
}
