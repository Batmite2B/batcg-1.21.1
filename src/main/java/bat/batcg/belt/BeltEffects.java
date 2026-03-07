package bat.batcg.belt;

import bat.batcg.Batcg;
import bat.batcg.card.CardTier;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import bat.batcg.advancement.BatcgAdvancements;
import net.minecraft.server.network.ServerPlayerEntity;

public final class BeltEffects {

    private BeltEffects() {}

    // IDs estables: se reemplazan cada tick sin acumular
    private static final Identifier ID_ATTACK      = Identifier.of(Batcg.MOD_ID, "belt_attack");
    private static final Identifier ID_ARMOR       = Identifier.of(Batcg.MOD_ID, "belt_armor");
    private static final Identifier ID_SPEED       = Identifier.of(Batcg.MOD_ID, "belt_speed");
    private static final Identifier ID_HEALTH      = Identifier.of(Batcg.MOD_ID, "belt_health");
    private static final Identifier ID_ATK_SPEED   = Identifier.of(Batcg.MOD_ID, "belt_attack_speed");
    private static final Identifier ID_LUCK        = Identifier.of(Batcg.MOD_ID, "belt_luck");
    private static final Identifier ID_KB_RES      = Identifier.of(Batcg.MOD_ID, "belt_knockback_res");

    public record Powers(
            double miningMultiplier,        // multiplicador final (1.0 = normal)
            double attackBonus,             // +value
            double armorBonus,              // +value
            double speedBonus,              // +mult total (0.02 = +2%)
            double maxHealthBonus,          // +value
            double attackSpeedBonus,        // +value
            double luckBonus,               // +value
            double knockbackResBonus        // +value
    ) {
        public static Powers empty() {
            return new Powers(1.0, 0,0,0,0,0,0,0);
        }
    }

    public static Powers compute(ItemStack belt) {
        if (belt == null || belt.isEmpty()) return Powers.empty();

        // Ajusta esto a gusto:
        // 0.35 = armor escala MUY poco
        // 0.45 = buen balance (recomendado)
        // 0.60 = armor escala bastante
        final double ARMOR_SCALE = 0.40;

        double mining = 0;
        double attack = 0;
        double armor  = 0;
        double speed  = 0;
        double health = 0;
        double atkSpd = 0;
        double luck   = 0;
        double kbRes  = 0;

        for (int i = 0; i < BeltCards.SLOTS; i++) {
            var s = BeltCards.get(belt, i);
            if (s == null || s.isEmpty()) continue;

            double mult = rarityMultiplier(s.tier());

            // ✅ Perks por carta (definidos en config)
            CardPerkTable.Perks p = CardPerkTable.get(s.pokemonId());
            if (p.isEmpty()) continue;

            // Escalado normal para la mayoría
            CardPerkTable.Perks scaled = p.scaled(mult);

            // ✅ Armor con escalado suave
            double armorMult = 1.0 + (mult - 1.0) * ARMOR_SCALE;
            armor += p.armorBonus() * armorMult;

            // Resto normal
            mining += scaled.miningBonus();
            attack += scaled.attackBonus();
            speed  += scaled.speedBonus();
            health += scaled.maxHealthBonus();
            atkSpd += scaled.attackSpeedBonus();
            luck   += scaled.luckBonus();
            kbRes  += scaled.knockbackResBonus();
        }

        double miningMultiplier = 1.0 + mining;
        return new Powers(miningMultiplier, attack, armor, speed, health, atkSpd, luck, kbRes);
    }

    /** Hidden effects: attributes don’t show as potion icons. */
    public static void apply(PlayerEntity player) {
        ItemStack belt = BatcgBeltApi.getEquippedBelt(player);
        Powers p = compute(belt);


        if (player instanceof ServerPlayerEntity sp && belt != null && !belt.isEmpty()) {
            if (BeltCards.getFilledCount(belt) == BeltCards.SLOTS) {
                BatcgAdvancements.grant(sp, "fill_belt", "done");
            }
        }


        applyModifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, ID_ATTACK, p.attackBonus(), EntityAttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, EntityAttributes.GENERIC_ARMOR, ID_ARMOR, p.armorBonus(), EntityAttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, ID_SPEED, p.speedBonus(), EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        applyModifier(player, EntityAttributes.GENERIC_MAX_HEALTH, ID_HEALTH, p.maxHealthBonus(), EntityAttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, EntityAttributes.GENERIC_ATTACK_SPEED, ID_ATK_SPEED, p.attackSpeedBonus(), EntityAttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, EntityAttributes.GENERIC_LUCK, ID_LUCK, p.luckBonus(), EntityAttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, ID_KB_RES, p.knockbackResBonus(), EntityAttributeModifier.Operation.ADD_VALUE);
    }

    public static double miningMultiplier(PlayerEntity player) {
        ItemStack belt = BatcgBeltApi.getEquippedBelt(player);
        return compute(belt).miningMultiplier();
    }

    /** Damage modifiers (tu sistema actual por tipo). Lo dejo igual. */
    public static float modifyIncomingDamage(PlayerEntity player, DamageSource source, float amount) {
        ItemStack belt = BatcgBeltApi.getEquippedBelt(player);
        if (belt.isEmpty()) return amount;

        PokemonType main = strongestType(belt);
        double t = Math.min(strongestWeight(belt), 8.0);

        if (main == PokemonType.FIRE) {
            if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_FIRE)) {
                return (float) (amount * (1.0 - 0.06 * t));
            }
            if (source.isOf(DamageTypes.DROWN)) {
                return (float) (amount * (1.0 + 0.04 * t));
            }
        }

        if (main == PokemonType.WATER) {
            if (source.isOf(DamageTypes.DROWN)) {
                return (float) (amount * (1.0 - 0.08 * t));
            }
            if (source.isOf(DamageTypes.LIGHTNING_BOLT)) {
                return (float) (amount * (1.0 + 0.05 * t));
            }
        }

        if (main == PokemonType.ELECTRIC) {
            if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_EXPLOSION)) {
                return (float) (amount * (1.0 + 0.03 * t));
            }
        }

        if (main == PokemonType.STEEL) {
            if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_PROJECTILE)) {
                return (float) (amount * (1.0 - 0.05 * t));
            }
            if (source.isOf(DamageTypes.LIGHTNING_BOLT)) {
                return (float) (amount * (1.0 + 0.04 * t));
            }
        }

        if (main == PokemonType.ROCK) {
            if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_EXPLOSION)) {
                return (float) (amount * (1.0 - 0.06 * t));
            }
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

        inst.removeModifier(id);
        if (Math.abs(value) < 1.0E-9) return;

        inst.addTemporaryModifier(new EntityAttributeModifier(id, value, op));
    }

    private static double rarityMultiplier(CardTier tier) {
        return switch (tier) {
            case COMMON -> 1.0;
            case UNCOMMON -> 1.25;
            case RARE -> 1.6;
            case EPIC -> 2.0;
            case LEGENDARY -> 2.6;
            case SHINY -> 3.4;
        };
    }

    private static PokemonType strongestType(ItemStack belt) {
        PokemonType best = PokemonType.NORMAL;
        double bestW = 0.0;
        for (int i = 0; i < BeltCards.SLOTS; i++) {
            var s = BeltCards.get(belt, i);
            if (s == null || s.isEmpty()) continue;
            PokemonType t = PokemonTypeIndex.getPrimaryType(s.pokemonId());
            double w = rarityMultiplier(s.tier());
            if (w > bestW) { bestW = w; best = t; }
        }
        return best;
    }

    private static double strongestWeight(ItemStack belt) {
        double bestW = 0.0;
        for (int i = 0; i < BeltCards.SLOTS; i++) {
            var s = BeltCards.get(belt, i);
            if (s == null || s.isEmpty()) continue;
            double w = rarityMultiplier(s.tier());
            if (w > bestW) bestW = w;
        }
        return bestW;
    }
}