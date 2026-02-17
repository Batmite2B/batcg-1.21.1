package bat.batcg.client;

import bat.batcg.belt.BatcgBeltApi;
import bat.batcg.network.payload.OpenBeltC2SPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class BatcgBeltKeybinds {

    private static KeyBinding OPEN_BELT;

    private BatcgBeltKeybinds() {}

    public static void init() {
        OPEN_BELT = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.batcg.open_belt",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.batcg"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_BELT.wasPressed()) {
                var player = MinecraftClient.getInstance().player;
                if (player == null) return;

                // Solo si está equipado
                if (!BatcgBeltApi.getEquippedBelt(player).isEmpty()) {
                    ClientPlayNetworking.send(new OpenBeltC2SPayload());
                }
            }
        });
    }
}
