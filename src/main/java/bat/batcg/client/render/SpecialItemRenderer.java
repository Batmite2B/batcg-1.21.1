package bat.batcg.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
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

import java.util.ArrayList;
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
        if (model == null) model = client.getBakedModelManager().getMissingModel();

        boolean gui = (mode == ModelTransformationMode.GUI);

        matrices.push();

        if (gui) {
            // GUI: lighting plana
            DiffuseLighting.disableGuiDepthLighting();
        }

        matrices.translate(0.5F, 0.5F, 0.5F);
        model.getTransformation().getTransformation(mode).apply(leftHanded, matrices);
        matrices.translate(-0.5F, -0.5F, -0.5F);

        afterTransformBeforeDraw.run();

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int actualLight = gui ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light;

        renderBakedModel(model, stack, matrices, vertexConsumers, actualLight, overlay, gui);

        if (gui) {
            DiffuseLighting.enableGuiDepthLighting();
        }

        matrices.pop();
    }

    private static void renderBakedModel(BakedModel model,
                                         ItemStack stack,
                                         MatrixStack matrices,
                                         VertexConsumerProvider vertexConsumers,
                                         int light,
                                         int overlay,
                                         boolean gui) {

        RenderLayer layer = RenderLayers.getItemLayer(stack, true);

        // En GUI suele verse mejor solid=false
        boolean solid = !gui;

        VertexConsumer vc = ItemRenderer.getDirectItemGlintConsumer(
                vertexConsumers, layer, solid, stack.hasGlint()
        );

        Random random = Random.create();
        random.setSeed(42L);

        if (gui) {
            // ✅ GUI: recolecta TODOS los quads (dir + null)
            List<BakedQuad> all = new ArrayList<>(64);

            for (Direction dir : Direction.values()) {
                all.addAll(model.getQuads(null, dir, random));
            }
            // IMPORTANTE: muchos modelos ponen TODO aquí
            all.addAll(model.getQuads(null, null, random));

            // ✅ intenta dibujar SOLO frente/atrás para eliminar laterales oscuros
            drawGuiFrontBackOrFallback(matrices, vc, all, light, overlay);
            return;
        }

        // Mundo/hand/ground: render normal
        for (Direction dir : Direction.values()) {
            random.setSeed(42L);
            drawQuads(matrices, vc, model.getQuads(null, dir, random), light, overlay, false);
        }

        random.setSeed(42L);
        drawQuads(matrices, vc, model.getQuads(null, null, random), light, overlay, false);
    }

    /**
     * GUI: dibuja solo quads NORTH/SOUTH para evitar sombreado de laterales.
     * Fallback: si no hay ninguno (por orientación), dibuja todos para no quedar invisible.
     */
    private static void drawGuiFrontBackOrFallback(MatrixStack matrices,
                                                   VertexConsumer vertices,
                                                   List<BakedQuad> all,
                                                   int light,
                                                   int overlay) {

        MatrixStack.Entry entry = matrices.peek();

        // 1) filtrar front/back
        boolean drewAny = false;
        for (BakedQuad quad : all) {
            Direction f = quad.getFace();
            if (f == Direction.NORTH || f == Direction.SOUTH) {
                emitQuadUnshaded(entry, vertices, quad, light, overlay);
                drewAny = true;
            }
        }

        // 2) fallback: si no encontró NORTH/SOUTH, dibuja todo sin shading (visible sí o sí)
        if (!drewAny) {
            for (BakedQuad quad : all) {
                emitQuadUnshaded(entry, vertices, quad, light, overlay);
            }
        }
    }

    private static void drawQuads(MatrixStack matrices,
                                  VertexConsumer vertices,
                                  List<BakedQuad> quads,
                                  int light,
                                  int overlay,
                                  boolean gui) {

        MatrixStack.Entry entry = matrices.peek();

        for (BakedQuad quad : quads) {
            if (gui) {
                emitQuadUnshaded(entry, vertices, quad, light, overlay);
            } else {
                vertices.quad(entry, quad, 1f, 1f, 1f, 1f, light, overlay);
            }
        }
    }

    /**
     * Emite el quad con color blanco completo (sin oscurecer) para GUI.
     */
    private static void emitQuadUnshaded(MatrixStack.Entry entry,
                                         VertexConsumer vc,
                                         BakedQuad quad,
                                         int light,
                                         int overlay) {

        int[] data = quad.getVertexData();
        Direction face = quad.getFace();

        float nx = face.getOffsetX();
        float ny = face.getOffsetY();
        float nz = face.getOffsetZ();

        // 8 ints por vértice * 4 = 32 ints (típico)
        for (int i = 0; i < 4; i++) {
            int base = i * 8;

            float x = Float.intBitsToFloat(data[base]);
            float y = Float.intBitsToFloat(data[base + 1]);
            float z = Float.intBitsToFloat(data[base + 2]);

            float u = Float.intBitsToFloat(data[base + 4]);
            float v = Float.intBitsToFloat(data[base + 5]);

            vc.vertex(entry.getPositionMatrix(), x, y, z);
            vc.color(255, 255, 255, 255);
            vc.texture(u, v);
            vc.overlay(overlay);
            vc.light(light);
            vc.normal(entry, nx, ny, nz);
        }
    }
}
