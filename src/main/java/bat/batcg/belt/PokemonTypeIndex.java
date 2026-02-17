package bat.batcg.belt;

import net.minecraft.util.Identifier;

import java.util.*;

public final class PokemonTypeIndex {

    private PokemonTypeIndex() {}

    private static final Map<String, PokemonType> FALLBACK = Map.ofEntries(
            Map.entry("charmander", PokemonType.FIRE),
            Map.entry("charmeleon", PokemonType.FIRE),
            Map.entry("charizard", PokemonType.FIRE),
            Map.entry("squirtle", PokemonType.WATER),
            Map.entry("wartortle", PokemonType.WATER),
            Map.entry("blastoise", PokemonType.WATER),
            Map.entry("bulbasaur", PokemonType.GRASS),
            Map.entry("ivysaur", PokemonType.GRASS),
            Map.entry("venusaur", PokemonType.GRASS),
            Map.entry("pikachu", PokemonType.ELECTRIC),
            Map.entry("raichu", PokemonType.ELECTRIC)
    );

    /** pokemonId is your card component string, e.g. "charmander" */
    public static PokemonType getPrimaryType(String pokemonId) {
        if (pokemonId == null) return PokemonType.NORMAL;
        return FALLBACK.getOrDefault(pokemonId.toLowerCase(Locale.ROOT), PokemonType.NORMAL);
    }
}
