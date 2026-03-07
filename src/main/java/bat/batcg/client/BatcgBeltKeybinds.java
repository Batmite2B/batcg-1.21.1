package bat.batcg.client;

import bat.batcg.belt.BatcgBeltApi;
import bat.batcg.network.payload.OpenBeltC2SPayload;
import bat.batcg.network.payload.UseLeaderPowerC2SPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class BatcgBeltKeybinds {

    private static KeyBinding OPEN_BELT;
    private static KeyBinding USE_POWER;

    private BatcgBeltKeybinds() {}

    public static void init() {
        OPEN_BELT = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.batcg.open_belt",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.batcg"
        ));

        USE_POWER = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.batcg.use_leader_power",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.batcg"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_BELT.wasPressed()) {
                if (client.player == null) return;
                if (!BatcgBeltApi.getEquippedBelt(client.player).isEmpty()) {
                    ClientPlayNetworking.send(new OpenBeltC2SPayload());
                }
            }

            while (USE_POWER.wasPressed()) {
                if (client.player == null) return;
                if (BatcgBeltApi.getEquippedBelt(client.player).isEmpty()) return;

                // leer input WASD actual (cliente) y mandarlo al server
                int f = 0;
                int s = 0;

                if (client.options.forwardKey.isPressed()) f += 1;
                if (client.options.backKey.isPressed())    f -= 1;

                // Nota: aquí "left" = A y "right" = D
                if (client.options.leftKey.isPressed())    s -= 1;  // A = izquierda real
                if (client.options.rightKey.isPressed())   s += 1;  // D = derecha real

                byte fb = (byte) Math.max(-1, Math.min(1, f));
                byte sb = (byte) Math.max(-1, Math.min(1, s));

                ClientPlayNetworking.send(new UseLeaderPowerC2SPayload(fb, sb));
            }
        });
    }
}