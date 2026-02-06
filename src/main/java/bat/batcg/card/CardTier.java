package bat.batcg.card;

import net.minecraft.util.Formatting;

public enum CardTier {

    COMMON("Common", Formatting.WHITE),
    UNCOMMON("Uncommon", Formatting.GREEN),
    RARE("Rare", Formatting.BLUE),
    EPIC("Epic", Formatting.DARK_PURPLE),
    LEGENDARY("Legendary", Formatting.GOLD),
    SHINY("Shiny", Formatting.AQUA);

    private final String displayName;
    private final Formatting color;

    CardTier(String displayName, Formatting color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Formatting getColor() {
        return color;
    }
}

