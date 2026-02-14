package bat.batcg.screen;

import bat.batcg.Batcg;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {
    private ModScreenHandlers() {}

    public static final ScreenHandlerType<BeltScreenHandler> BELT =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    Identifier.of(Batcg.MOD_ID, "belt"),
                    new ScreenHandlerType<>(BeltScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
            );

    public static void init() {}
}
