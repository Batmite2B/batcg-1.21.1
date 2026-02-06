package bat.batcg.command;

import bat.batcg.card.CardIdIndex;
import bat.batcg.card.CardTier;
import bat.batcg.item.PokemonCardItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.item.ItemStack;

public final class ModCommands {

    private ModCommands() {}

    // Sugerencias para IDs (TAB)
    private static final SuggestionProvider<ServerCommandSource> CARD_ID_SUGGESTIONS =
            (ctx, builder) -> CommandSource.suggestMatching(CardIdIndex.all(), builder);

    // Sugerencias para Tiers (TAB)
    private static final SuggestionProvider<ServerCommandSource> TIER_SUGGESTIONS =
            (ctx, builder) -> {
                for (CardTier t : CardTier.values()) {
                    builder.suggest(t.name().toLowerCase());
                }
                return builder.buildFuture();
            };

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerGiveCard(dispatcher);
        });
    }

    private static void registerGiveCard(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("givecard")
                .then(CommandManager.argument("pokemonId", StringArgumentType.word())
                        .suggests(CARD_ID_SUGGESTIONS)
                        .then(CommandManager.argument("tier", StringArgumentType.word())
                                .suggests(TIER_SUGGESTIONS)
                                .executes(ctx -> {

                                    var player = ctx.getSource().getPlayer(); // comando en jugador
                                    if (player == null) {
                                        ctx.getSource().sendError(Text.literal("Este comando solo lo puede usar un jugador."));
                                        return 0;
                                    }

                                    // Lee args
                                    String rawId = StringArgumentType.getString(ctx, "pokemonId");
                                    String id = CardIdIndex.normalize(rawId);

                                    String tierStr = StringArgumentType.getString(ctx, "tier").trim().toUpperCase();

                                    // Valida tier
                                    CardTier tier;
                                    try {
                                        tier = CardTier.valueOf(tierStr);
                                    } catch (Exception e) {
                                        ctx.getSource().sendError(Text.literal("Tier inválido. Usa: common, uncommon, rare, epic, legendary, shiny"));
                                        return 0;
                                    }

                                    // Regla SHINY: icon shiny solo con tier SHINY, y tier SHINY solo con icon shiny
                                    boolean isShinyId = id.endsWith("shiny");
                                    if (tier == CardTier.SHINY && !isShinyId) {
                                        ctx.getSource().sendError(Text.literal("Ese tier es SHINY, pero el id no termina en 'shiny'. Ej: 001bulbasaurshiny")
                                                .formatted(Formatting.RED));
                                        return 0;
                                    }
                                    if (tier != CardTier.SHINY && isShinyId) {
                                        ctx.getSource().sendError(Text.literal("Ese id es SHINY, pero el tier no es shiny. Usa tier: shiny")
                                                .formatted(Formatting.RED));
                                        return 0;
                                    }

                                    // Valida ID existente (evita crasheos por modelo/textura inexistente)
                                    if (!CardIdIndex.exists(id)) {
                                        ctx.getSource().sendError(Text.literal("Pokemon inválido: " + id).formatted(Formatting.RED));
                                        return 0;
                                    }

                                    // Crea la carta
                                    ItemStack card = PokemonCardItem.createCard(id, tier);

                                    // ✅ Inserta al inventario; si no cabe, dropea al suelo
                                    boolean inserted = player.getInventory().insertStack(card);
                                    if (!inserted) {
                                        player.dropItem(card, false);
                                    }

                                    final String finalMsgId = id;
                                    final String finalMsgTier = tier.name().toLowerCase();

                                    ctx.getSource().sendFeedback(
                                            () -> Text.literal("Carta Recibida: " + finalMsgId + " (" + finalMsgTier + ")")
                                                    .formatted(Formatting.GREEN),
                                            false
                                    );

                                    return 1;
                                }))));
    }
}
