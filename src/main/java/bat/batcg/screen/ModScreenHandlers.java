package bat.batcg.screen;

import bat.batcg.Batcg;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {

    public static final ScreenHandlerType<BeltScreenHandler> BELT =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    Identifier.of(Batcg.MOD_ID, "belt"),
                    new ScreenHandlerType<>(BeltScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
            );

    public static final ScreenHandlerType<GradeStationScreenHandler> GRADE_STATION =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    Identifier.of(Batcg.MOD_ID, "grade_station"),
                    new ScreenHandlerType<>(GradeStationScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
            );

    private ModScreenHandlers() {}

    public static void init() { }
}