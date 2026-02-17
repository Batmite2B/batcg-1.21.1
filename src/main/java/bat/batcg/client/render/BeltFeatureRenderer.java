package bat.batcg.client.render;

import bat.batcg.belt.BatcgBeltApi;
import bat.batcg.belt.BeltCards;
import bat.batcg.item.PokemonCardItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public class BeltFeatureRenderer
        extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public BeltFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            AbstractClientPlayerEntity player,
            float limbAngle,
            float limbDistance,
            float tickDelta,
            float animationProgress,
            float headYaw,
            float headPitch
    ) {
        ItemStack belt = BatcgBeltApi.getEquippedBelt(player);
        if (belt == null || belt.isEmpty()) return;

        ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();

        matrices.push();

        // 1) Anclar al torso
        this.getContextModel().body.rotate(matrices);

        // 2) Posición del cinturón
        matrices.translate(0.0D, 0.80D, -0.02D);

        // 3) Orientación del cinturón
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180f));

        // 4) Escala del cinturón
        float beltScale = 0.90f;
        matrices.scale(beltScale, beltScale, beltScale);

        // Render BELT
        itemRenderer.renderItem(
                player,
                belt,
                ModelTransformationMode.NONE,
                false,
                matrices,
                vertexConsumers,
                player.getWorld(),
                light,
                OverlayTexture.DEFAULT_UV,
                player.getId()
        );

        // ---- CARD ----
        BeltCards.SlotData first = BeltCards.getFirstFilled(belt);
        if (first == null || first.isEmpty()) {
            matrices.pop();
            return;
        }

        ItemStack card = PokemonCardItem.createCard(first.pokemonId(), first.tier());

        matrices.push();

        // Posición relativa al cinturón (sube y sácala hacia afuera)
        matrices.translate(-0.155D, 0.185D, -0.225D);

        // MUY IMPORTANTE:
        // Deshacer las rotaciones del cinturón para que la carta no quede “de espaldas/culled”
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0f));

        // Orientación final: que mire hacia afuera
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0f));

        // Escala de la carta (ponla GRANDE primero para confirmar que aparece)
        float cardScale = 0.22f;
        matrices.scale(cardScale, cardScale, cardScale);

        // FIXED se parece a item-frame / mundo y suele mostrar mejor modelos planos
        itemRenderer.renderItem(
                player,
                card,
                ModelTransformationMode.FIXED,
                false,
                matrices,
                vertexConsumers,
                player.getWorld(),
                light,
                OverlayTexture.DEFAULT_UV,
                player.getId() + 1337
        );

        matrices.pop();
        matrices.pop();
    }
}
