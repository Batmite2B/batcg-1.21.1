package bat.batcg.belt;

import java.util.Locale;
import java.util.Map;

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

    public static PokemonType getPrimaryType(String pokemonId) {
        if (pokemonId == null) return PokemonType.NORMAL;

        String clean = pokemonId.trim().toLowerCase(Locale.ROOT);

        // por si alguien usa namespace
        int colon = clean.indexOf(':');
        if (colon >= 0 && colon < clean.length() - 1) clean = clean.substring(colon + 1);

        // por si viene con shiny
        if (clean.endsWith("shiny")) clean = clean.substring(0, clean.length() - "shiny".length());

        // quita prefijo numérico (001bulbasaur -> bulbasaur)
        clean = clean.replaceFirst("^\\d+", "");

        return FALLBACK.getOrDefault(clean, PokemonType.NORMAL);
    }
}