package bat.batcg.belt;

import bat.batcg.card.CardTier;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import bat.batcg.Batcg;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;


import java.util.EnumMap;
import java.util.Map;

public final class BeltEffects {

    private BeltEffects() {}

    // stable UUIDs so we can replace modifiers every tick without stacking
    private static final Identifier ID_ATTACK = Identifier.of(Batcg.MOD_ID, "belt_attack");
    private static final Identifier ID_ARMOR  = Identifier.of(Batcg.MOD_ID, "belt_armor");
    private static final Identifier ID_SPEED  = Identifier.of(Batcg.MOD_ID, "belt_speed");


    public record Powers(
            double miningMultiplier,
            double attackBonus,
            double armorBonus,
            double speedBonus
    ) {
        public static Powers empty() { return new Powers(1.0, 0.0, 0.0, 0.0); }
    }

    public static Powers compute(ItemStack belt) {
        if (belt == null || belt.isEmpty()) return Powers.empty();

        // Sum contributions per type (rarity-weighted)
        Map<PokemonType, Double> weight = new EnumMap<>(PokemonType.class);

        for (int i = 0; i < 5; i++) {
            var s = BeltCards.get(belt, i);
            if (s == null) continue;
            PokemonType type = PokemonTypeIndex.getPrimaryType(s.pokemonId());
            double w = rarityMultiplier(s.tier());
            weight.merge(type, w, Double::sum);
        }

        if (weight.isEmpty()) return Powers.empty();

        // Pick a simple, consistent design:
        // - Main buff comes from your strongest type
        // - Debuff comes from a "cost" of that type (handled in damage modifier)
        PokemonType main = PokemonType.NORMAL;
        double mainW = 0.0;
        for (var e : weight.entrySet()) {
            if (e.getValue() > mainW) { main = e.getKey(); mainW = e.getValue(); }
        }

        // scale: common ~0.6, shiny ~1.8; sum could be up to ~9
        double t = Math.min(mainW, 8.0);

                return switch (main) {
            case ELECTRIC -> new Powers(1.0 + 0.06 * t, 0.0, 0.0, -0.002 * t);             // mine faster, slightly slower
            case FIGHTING -> new Powers(1.0, 0.35 * t, -0.12 * t, 0.0);                     // more melee, less armor
            case STEEL -> new Powers(1.0, 0.0, 0.7 * t, -0.003 * t);                        // more armor, slower
            case FLYING -> new Powers(1.0, 0.0, -0.15 * t, 0.010 * t);                      // faster, less armor
            case FIRE -> new Powers(1.0, 0.20 * t, 0.15 * t, 0.0);                          // some dmg + armor, weakness handled in damage
            case WATER -> new Powers(1.0, 0.0, 0.30 * t, -0.002 * t);                       // tanky, a bit slower
            case GRASS -> new Powers(1.0, 0.0, 0.40 * t, -0.003 * t);                       // tanky, slower
            case ROCK -> new Powers(1.0, 0.0, 0.75 * t, -0.006 * t);                        // very tanky, slower
            case ICE -> new Powers(1.0, 0.30 * t, -0.20 * t, -0.004 * t);                   // dmg, fragile, slower
            case PSYCHIC -> new Powers(1.0, 0.25 * t, -0.10 * t, 0.004 * t);                // dmg+speed, slightly fragile
            case DARK -> new Powers(1.0, 0.30 * t, -0.15 * t, 0.0);                         // dmg, less armor
            case DRAGON -> new Powers(1.0, 0.30 * t, 0.30 * t, -0.002 * t);                 // strong, heavy
            case FAIRY -> new Powers(1.0, -0.10 * t, 0.30 * t, 0.006 * t);                  // supporty, less dmg
            case GROUND -> new Powers(1.0, 0.0, 0.45 * t, -0.004 * t);                      // sturdy, slower
            case POISON -> new Powers(1.0, 0.25 * t, -0.15 * t, 0.002 * t);                 // dmg, fragile-ish
            case BUG -> new Powers(1.0, 0.18 * t, -0.10 * t, 0.006 * t);                    // agile, a bit fragile
            case GHOST -> new Powers(1.0, 0.22 * t, -0.15 * t, 0.007 * t);                  // mobile, fragile
            case NORMAL -> new Powers(1.0, 0.12 * t, 0.12 * t, -0.001 * t);                 // mild, tiny cost
        };
    }

    /** Hidden effects: attributes don't show as potion icons. */
    public static void apply(PlayerEntity player) {
        ItemStack belt = BatcgBeltApi.getEquippedBelt(player);
        Powers p = compute(belt);

        applyModifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, ID_ATTACK, p.attackBonus(), EntityAttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, EntityAttributes.GENERIC_ARMOR, ID_ARMOR, p.armorBonus(), EntityAttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, ID_SPEED, p.speedBonus(), EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    }

    public static double miningMultiplier(PlayerEntity player) {
        ItemStack belt = BatcgBeltApi.getEquippedBelt(player);
        return compute(belt).miningMultiplier();
    }

    /** Damage modifiers: keep it simple and "type flavored". No potion effects. */
    public static float modifyIncomingDamage(PlayerEntity player, DamageSource source, float amount) {
        ItemStack belt = BatcgBeltApi.getEquippedBelt(player);
        if (belt.isEmpty()) return amount;

        // strongest type decides resist/weakness
        PokemonType main = strongestType(belt);
        double t = Math.min(strongestWeight(belt), 8.0);

        // Example: FIRE resists fire, but takes a bit more water / freezing.
        if (main == PokemonType.FIRE) {
            if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_FIRE)) {
                return (float) (amount * (1.0 - 0.06 * t));
            }
            // debuff: extra damage when wet-like sources (drowning)
            if (source.isOf(DamageTypes.DROWN)) {
                return (float) (amount * (1.0 + 0.04 * t));
            }
        }

        if (main == PokemonType.WATER) {
            if (source.isOf(DamageTypes.DROWN)) {
                return (float) (amount * (1.0 - 0.08 * t));
            }
            // debuff: lightning hurts more
            if (source.isOf(DamageTypes.LIGHTNING_BOLT)) {
                return (float) (amount * (1.0 + 0.05 * t));
            }
        }

        if (main == PokemonType.ELECTRIC) {
            // debuff: a bit more damage from explosions (overload)
            if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_EXPLOSION)) {
                return (float) (amount * (1.0 + 0.03 * t));
            }
        }

        if (main == PokemonType.STEEL) {
            // resist projectiles
            if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_PROJECTILE)) {
                return (float) (amount * (1.0 - 0.05 * t));
            }
            // debuff: lightning hurts steel armor
            if (source.isOf(DamageTypes.LIGHTNING_BOLT)) {
                return (float) (amount * (1.0 + 0.04 * t));
            }
        }

        if (main == PokemonType.ROCK) {
            // resist explosions
            if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_EXPLOSION)) {
                return (float) (amount * (1.0 - 0.06 * t));
            }
            // debuff: more damage from pickaxes? (no clean tag) -> more fall damage
            if (source.isOf(DamageTypes.FALL)) {
                return (float) (amount * (1.0 + 0.03 * t));
            }
        }

        return amount;
    }

    // ---------------- helpers ----------------

    private static void applyModifier(
            PlayerEntity player,
            RegistryEntry<EntityAttribute> attribute,
            Identifier id,
            double value,
            EntityAttributeModifier.Operation op
    ) {
        EntityAttributeInstance inst = player.getAttributeInstance(attribute);
        if (inst == null) return;

        // Limpia el modifier anterior del mismo id (si existía)
        inst.removeModifier(id);

        // Si no hay bono, no agregues nada
        if (Math.abs(value) < 1.0E-9) return;

        // 1.21.x: modifiers van por Identifier, sin "name" y sin UUID
        inst.addTemporaryModifier(new EntityAttributeModifier(id, value, op));
    }


    private static double rarityMultiplier(CardTier tier) {

        if (tier == null) return 1.0f;

        return switch (tier) {
            case COMMON -> 0.6;
            case UNCOMMON -> 0.9;
            case RARE -> 1.15;
            case EPIC -> 1.4;
            case LEGENDARY -> 1.7;
            case SHINY -> 2.0;
        };
    }

    private static PokemonType strongestType(ItemStack belt) {
        PokemonType best = PokemonType.NORMAL;
        double bestW = 0.0;
        for (int i = 0; i < 5; i++) {
            var s = BeltCards.get(belt, i);
            if (s == null) continue;
            PokemonType t = PokemonTypeIndex.getPrimaryType(s.pokemonId());
            double w = rarityMultiplier(s.tier());
            if (w > bestW) { bestW = w; best = t; }
        }
        return best;
    }

    private static double strongestWeight(ItemStack belt) {
        double bestW = 0.0;
        for (int i = 0; i < 5; i++) {
            var s = BeltCards.get(belt, i);
            if (s == null) continue;
            double w = rarityMultiplier(s.tier());
            if (w > bestW) bestW = w;
        }
        return bestW;
    }
}
