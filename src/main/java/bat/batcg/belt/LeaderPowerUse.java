package bat.batcg.belt;

import bat.batcg.belt.LeaderPowerTable.LeaderPower;
import bat.batcg.belt.LeaderPowerTable.PowerId;
import bat.batcg.card.CardTier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class LeaderPowerUse {

    private LeaderPowerUse() {}

    private static final String KEY_CD_UNTIL = "batcg_leader_cd_until";

    public static void tryUse(ServerPlayerEntity player, byte forward, byte strafe) {
        ItemStack belt = BatcgBeltApi.getEquippedBelt(player);
        if (belt == null || belt.isEmpty()) {
            player.sendMessage(Text.literal("No tienes el belt equipado."), true);
            return;
        }

        BeltCards.SlotData leader = BeltCards.get(belt, 0);
        String leaderId = (leader == null) ? "" : leader.pokemonId();
        if (leaderId == null || leaderId.isBlank()) {
            player.sendMessage(Text.literal("Tu slot 1 (líder) está vacío."), true);
            return;
        }

        // ✅ BLOQUEO POR GRADE (None=0, C=1+)
        int g = (leader == null) ? 0 : leader.grade();
        if (g < 1) {
            player.sendMessage(Text.literal("Poder oculto bloqueado. Gradea esta carta (mínimo C)."), true);
            return;
        }

        LeaderPower base = LeaderPowerTable.get(leaderId);
        if (base.isNone()) {
            player.sendMessage(Text.literal("Este Pokémon no tiene poder asignado: " + leaderId), true);
            return;
        }

        long now = player.getWorld().getTime();

        // Leer cooldown actual
        NbtCompound tag = getCustomData(belt);
        long cdUntil = tag.getLong(KEY_CD_UNTIL);

        if (now < cdUntil) {
            double left = (cdUntil - now) / 20.0;
            player.sendMessage(Text.literal(String.format("En cooldown: %.1fs", left)), true);
            return;
        }

        CardTier tier = leader.tier();
        LeaderPower power = scaleByTier(base, tier);

        boolean ok = execute(player, belt, power, forward, strafe);
        if (!ok) return;

        // ✅ CRÍTICO: re-leer NBT DESPUÉS de execute()
        // para NO borrar "batcg_active_power" y otros campos que el execute haya escrito.
        NbtCompound tag2 = getCustomData(belt);
        tag2.putLong(KEY_CD_UNTIL, now + power.cooldownTicks());
        setCustomData(belt, tag2);
    }

    /** Escalado: value + duration + radius por tier; cooldown baja en tiers altos. */
    private static LeaderPower scaleByTier(LeaderPower p, CardTier tier) {
        if (tier == null) return p;

        double mult = switch (tier) {
            case COMMON -> 1.00;
            case UNCOMMON -> 1.08;
            case RARE -> 1.15;
            case EPIC -> 1.25;
            case LEGENDARY -> 1.35;
            case SHINY -> 1.45;
        };

        double cdMult = switch (tier) {
            case COMMON -> 1.00;
            case UNCOMMON -> 0.95;
            case RARE -> 0.92;
            case EPIC -> 0.88;
            case LEGENDARY -> 0.84;
            case SHINY -> 0.80;
        };

        int dur = (int) Math.round(p.durationTicks() * (1.0 + (mult - 1.0) * 0.60));
        int cd  = (int) Math.round(p.cooldownTicks() * cdMult);

        double radius = p.radius() * (1.0 + (mult - 1.0) * 1.25);

        double value = p.value();
        if (p.id() == PowerId.BLINK || p.id() == PowerId.ARCANE_SHIFT) {
            value = p.value() + (mult - 1.0) * 10.0;
        } else {
            value = p.value() * mult;
        }

        return new LeaderPower(p.id(), value, radius, dur, Math.max(5, cd), p.amp());
    }

    private static boolean execute(ServerPlayerEntity player, ItemStack belt, LeaderPower p, byte forward, byte strafe) {
        ServerWorld world = player.getServerWorld();

        switch (p.id()) {
            case HEAL -> {
                player.heal((float) p.value());
                int regen = p.durationTicks() > 0 ? p.durationTicks() : 140;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, regen, Math.max(0, p.amp()), true, false, true));

                play(world, player, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.65f, 1.2f);
                world.spawnParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.45, 0.55, 0.45, 0.02);
                return true;
            }

            case DASH -> {
                Vec3d dir = inputDirection(player, forward, strafe);
                double strength = p.value() * 1.35;
                player.addVelocity(dir.x * strength, 0.08, dir.z * strength);
                player.velocityModified = true;

                play(world, player, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 0.65f, 1.55f);
                world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 16, 0.32, 0.06, 0.32, 0.03);
                return true;
            }

            case WATER_JET -> {
                Vec3d dir = inputDirection(player, forward, strafe);
                double strength = p.value() * 1.35;
                player.addVelocity(dir.x * strength, 0.08, dir.z * strength);
                player.velocityModified = true;

                play(world, player, SoundEvents.ENTITY_DOLPHIN_SPLASH, 0.75f, 1.4f);
                world.spawnParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 0.2, player.getZ(), 20, 0.40, 0.10, 0.40, 0.06);
                world.spawnParticles(ParticleTypes.BUBBLE, player.getX(), player.getY() + 0.9, player.getZ(), 14, 0.40, 0.12, 0.40, 0.02);
                return true;
            }

            case CHARGE -> {
                Vec3d forwardDir = flatForward(player);
                double strength = p.value() * 2.15;
                player.addVelocity(forwardDir.x * strength, 0.10, forwardDir.z * strength);
                player.velocityModified = true;

                double hitRadius = Math.max(2.8, p.radius());
                double dmg = 5.0 + (p.amp() * 1.6);
                aoeDamageAndKnockback(player, hitRadius, dmg, Math.max(3, p.amp() + 2));

                play(world, player, SoundEvents.ENTITY_RAVAGER_STEP, 0.95f, 0.9f);
                world.spawnParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.1, player.getZ(), 22, 0.65, 0.12, 0.65, 0.02);
                world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 0.2, player.getZ(), 12, 0.45, 0.12, 0.45, 0.01);
                return true;
            }

            case DRAGON_RUSH -> {
                double r = 4.6;
                aoeDamageAndKnockback(player, r, 4.5, 4);

                int dur = Math.max(160, p.durationTicks() + 100);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, dur, Math.max(1, p.amp() + 1), true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, dur, 1, true, false, true));

                play(world, player, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.15f);
                world.spawnParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + 1.0, player.getZ(), 45, 1.1, 0.6, 1.1, 0.02);
                world.spawnParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.3, player.getZ(), 28, 1.0, 0.3, 1.0, 0.06);
                return true;
            }

            case TAILWIND -> {
                int dur = Math.max(20, (int) (p.durationTicks() * 1.45));
                int amp = Math.max(1, p.amp() + 1);

                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, dur, amp, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, dur, 1, true, false, true));
                play(world, player, SoundEvents.ITEM_ELYTRA_FLYING, 0.65f, 1.2f);
                world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 28, 0.95, 0.40, 0.95, 0.02);
                return true;
            }

            case BARRIER -> {
                int dur = Math.max(20, p.durationTicks());
                int absAmp = Math.max(0, (int) Math.round(p.value() / 4.0) - 1);

                player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, dur, absAmp, true, false, true));
                if (p.amp() > 0) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, dur, p.amp(), true, false, true));
                }

                play(world, player, SoundEvents.ITEM_SHIELD_BLOCK, 0.85f, 1.0f);
                world.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 18, 0.6, 0.7, 0.6, 0.03);
                return true;
            }

            case REGEN_AURA -> {
                player.heal(2.0f);
                int dur = Math.max(140, p.durationTicks());
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, dur, Math.max(1, p.amp()), true, false, true));

                player.removeStatusEffect(StatusEffects.POISON);
                player.removeStatusEffect(StatusEffects.WEAKNESS);
                player.removeStatusEffect(StatusEffects.SLOWNESS);
                player.removeStatusEffect(StatusEffects.WITHER);
                player.removeStatusEffect(StatusEffects.HUNGER);

                play(world, player, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.75f, 1.6f);
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 26, 1.0, 0.7, 1.0, 0.02);
                world.spawnParticles(ParticleTypes.COMPOSTER, player.getX(), player.getY() + 0.6, player.getZ(), 20, 1.1, 0.25, 1.1, 0.02);
                return true;
            }

            case ENTANGLE -> {
                double r = Math.max(3.8, p.radius());
                int dur = Math.max(120, p.durationTicks());

                aoeDebuffMobs(player, r, dur, StatusEffects.SLOWNESS, Math.max(2, p.amp() + 1));
                aoePoisonSmart(player, r, dur, Math.max(0, p.amp()));

                play(world, player, SoundEvents.BLOCK_GRASS_PLACE, 0.9f, 0.85f);
                world.spawnParticles(ParticleTypes.COMPOSTER, player.getX(), player.getY() + 0.6, player.getZ(), 34, 1.1, 0.25, 1.1, 0.02);
                return true;
            }

            case SLEEP_PULSE -> {
                double r = Math.max(4.2, p.radius());
                int dur = Math.max(140, p.durationTicks());

                aoeDebuffMobs(player, r, dur, StatusEffects.SLOWNESS, 2);
                aoeDebuffMobs(player, r, dur, StatusEffects.WEAKNESS, Math.max(2, p.amp() + 1));
                aoeDebuffMobs(player, r, Math.min(dur, 90), StatusEffects.BLINDNESS, 0);

                play(world, player, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 0.65f, 1.3f);
                world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, player.getX(), player.getY() + 1.0, player.getZ(), 44, 1.25, 0.55, 1.25, 0.02);
                return true;
            }

            case VENOM_CLOUD -> {
                double r = Math.max(3.8, p.radius());
                int dur = Math.max(140, p.durationTicks());
                aoePoisonSmart(player, r, dur, Math.max(1, p.amp()));

                play(world, player, SoundEvents.ENTITY_WITCH_THROW, 0.75f, 0.9f);
                world.spawnParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 46, 1.25, 0.70, 1.25, 0.02);
                return true;
            }

            case INTIMIDATE_ROAR -> {
                double r = Math.max(4.5, p.radius());
                int dur = Math.max(140, p.durationTicks());
                int amp = Math.max(1, p.amp() + 1);

                aoeDebuffMobs(player, r, dur, StatusEffects.WEAKNESS, amp);

                play(world, player, SoundEvents.ENTITY_RAVAGER_ROAR, 0.75f, 1.15f);
                world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 30, 1.1, 0.45, 1.1, 0.02);
                return true;
            }

            case SHOCK_NOVA -> {
                double r = Math.max(3.0, p.radius());
                aoeDamageAndDebuff(player, r, p.value(), Math.max(80, p.durationTicks()), StatusEffects.SLOWNESS, Math.max(1, p.amp()));

                play(world, player, SoundEvents.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.8f);
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(), 55, 1.1, 0.5, 1.1, 0.06);
                return true;
            }

            case ICE_NOVA -> {
                double r = Math.max(3.2, p.radius());
                aoeDamageAndDebuff(player, r, p.value(), Math.max(100, p.durationTicks()), StatusEffects.SLOWNESS, Math.max(2, p.amp()));

                play(world, player, SoundEvents.BLOCK_GLASS_BREAK, 0.8f, 1.2f);
                world.spawnParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0, player.getZ(), 50, 1.2, 0.6, 1.2, 0.02);
                return true;
            }

            case SLAM -> {
                double r = Math.max(3.2, p.radius());
                double dmg = p.value() * 1.75;
                aoeDamageAndKnockback(player, r, dmg, Math.max(3, p.amp() + 1));

                play(world, player, SoundEvents.ENTITY_IRON_GOLEM_ATTACK, 0.9f, 0.7f);
                world.spawnParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.2, player.getZ(), 12, 1.05, 0.35, 1.05, 0.04);
                world.spawnParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.2, player.getZ(), 32, 1.15, 0.35, 1.15, 0.06);
                return true;
            }

            case FIRE_BREATH -> {
                if (belt == null || belt.isEmpty()) return false;

                double range = Math.max(5.0, p.radius());
                double dmg = p.value();
                int channelTicks = Math.max(40, p.durationTicks());
                int fireTicksPerHit = 40;

                LeaderActivePowers.startFireBreath(belt, player, range, dmg, 0.60, fireTicksPerHit, channelTicks);

                play(world, player, SoundEvents.ITEM_FIRECHARGE_USE, 0.85f, 1.05f);
                return true;
            }

            case BUBBLE_BEAM -> {
                if (belt == null || belt.isEmpty()) return false;

                double range = Math.max(5.5, p.radius());
                double dmg = p.value();
                int channelTicks = Math.max(40, p.durationTicks());
                int debuffTicks = Math.max(60, p.durationTicks());
                int amp = Math.max(1, p.amp());

                LeaderActivePowers.startBubbleBeam(belt, player, range, dmg, 0.65, debuffTicks, amp, channelTicks);

                play(world, player, SoundEvents.ENTITY_DOLPHIN_SPLASH, 0.85f, 1.35f);
                return true;
            }

            case BLINK -> {
                int range = (int) Math.max(6, Math.round(p.value()));
                HitResult hit = player.raycast(range, 1.0f, false);
                Vec3d to = hit.getPos();

                Vec3d look = player.getRotationVec(1.0f).normalize();
                Vec3d dest = to.subtract(look.multiply(0.6));

                player.teleport(world, dest.x, dest.y, dest.z, player.getYaw(), player.getPitch());

                play(world, player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.85f, 1.2f);
                world.spawnParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1.0, dest.z, 48, 0.65, 0.65, 0.65, 0.10);
                return true;
            }

            case SHADOW_PHASE -> {
                int dur = Math.max(120, p.durationTicks());
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, dur, 0, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, dur, Math.max(1, p.amp()), true, false, true));

                play(world, player, SoundEvents.ENTITY_PHANTOM_FLAP, 0.75f, 0.8f);
                world.spawnParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 22, 0.65, 0.35, 0.65, 0.02);
                return true;
            }

            case MAGNET -> {
                if (belt == null || belt.isEmpty()) return false;

                double r = Math.max(8.0, p.radius());
                int channelTicks = Math.max(60, p.durationTicks());

                LeaderActivePowers.startMagnet(belt, player, r, channelTicks);

                play(world, player, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 0.85f, 0.95f);
                return true;
            }

            case WATER_HOP -> {
                double up = Math.max(0.6, p.value());
                player.addVelocity(0, up, 0);
                player.velocityModified = true;

                int dur = Math.max(80, p.durationTicks());
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, dur, 0, true, false, true));

                play(world, player, SoundEvents.ENTITY_SALMON_FLOP, 0.95f, 1.2f);
                world.spawnParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 0.2, player.getZ(), 24, 0.6, 0.25, 0.6, 0.04);
                return true;
            }

            case MIMIC -> {
                int dur = (int) (Math.max(120, p.durationTicks()) * 1.6);
                applyRandomBuff(player, dur);

                play(world, player, SoundEvents.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 0.75f, 1.4f);
                world.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 18, 0.65, 0.75, 0.65, 0.03);
                return true;
            }

            case ARCANE_SHIFT -> {
                int range = (int) Math.max(8, Math.round(p.value()));
                HitResult hit = player.raycast(range, 1.0f, false);
                Vec3d to = hit.getPos();
                Vec3d look = player.getRotationVec(1.0f).normalize();
                Vec3d dest = to.subtract(look.multiply(0.6));

                player.teleport(world, dest.x, dest.y, dest.z, player.getYaw(), player.getPitch());

                int dur = (int) (Math.max(120, p.durationTicks()) * 1.6);
                applyRandomBuff(player, dur);

                play(world, player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.85f, 1.3f);
                world.spawnParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1.0, dest.z, 56, 0.7, 0.7, 0.7, 0.11);
                return true;
            }

            case PSYCHIC_CRUSH -> {
                double r = Math.max(7.0, p.radius() + 2.0);
                int dur = Math.max(240, p.durationTicks() + 120);

                double dmg = p.value() * 1.35;
                int kb = Math.max(5, p.amp() + 2);
                aoeDamageAndKnockback(player, r, dmg, kb);

                int levAmp = Math.min(4, Math.max(1, p.amp() + 1));
                aoeDebuffMobs(player, r, dur, StatusEffects.LEVITATION, levAmp);
                aoeDebuffMobs(player, r, dur, StatusEffects.WEAKNESS, 1);
                aoeDebuffMobs(player, r, dur, StatusEffects.SLOWNESS, 1);

                play(world, player, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.85f, 1.35f);
                world.spawnParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1.0, player.getZ(), 30, 1.4, 0.9, 1.4, 0.06);
                world.spawnParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 220, 1.9, 1.2, 1.9, 0.14);
                world.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 80, 1.4, 0.8, 1.4, 0.05);
                return true;
            }

            default -> {
                player.sendMessage(Text.literal("Poder aún no implementado: " + p.id()), true);
                return false;
            }
        }
    }

    // ----------------- Helpers -----------------

    private static SoundEvent snd(Object maybe) {
        if (maybe instanceof SoundEvent se) return se;
        if (maybe instanceof RegistryEntry<?> re && re.value() instanceof SoundEvent se2) return se2;
        return null;
    }

    private static void play(ServerWorld world, ServerPlayerEntity player, Object soundConst, float vol, float pitch) {
        SoundEvent s = snd(soundConst);
        if (s == null) return;
        world.playSound(null, player.getBlockPos(), s, SoundCategory.PLAYERS, vol, pitch);
    }

    private static Vec3d flatForward(ServerPlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d f = new Vec3d(look.x, 0, look.z);
        if (f.lengthSquared() < 1.0e-6) return new Vec3d(0, 0, 1);
        return f.normalize();
    }

    private static Vec3d inputDirection(ServerPlayerEntity player, byte forward, byte strafe) {
        int f = forward;
        int s = strafe;
        if (f == 0 && s == 0) f = 1;

        Vec3d forwardDir = flatForward(player);
        Vec3d right = new Vec3d(-forwardDir.z, 0, forwardDir.x);

        Vec3d dir = forwardDir.multiply(f).add(right.multiply(s));
        if (dir.lengthSquared() < 1.0e-6) return forwardDir;
        return dir.normalize();
    }

    private static void aoeDebuffMobs(ServerPlayerEntity player, double radius, int durationTicks, RegistryEntry<StatusEffect> effect, int amp) {
        if (radius <= 0) return;
        ServerWorld world = player.getServerWorld();
        Box box = player.getBoundingBox().expand(radius);
        List<Entity> list = world.getOtherEntities(player, box, e -> e instanceof LivingEntity && !(e instanceof net.minecraft.entity.player.PlayerEntity));
        for (Entity e : list) {
            LivingEntity le = (LivingEntity) e;
            le.addStatusEffect(new StatusEffectInstance(effect, Math.max(40, durationTicks), Math.max(0, amp)));
        }
    }

    private static void aoePoisonSmart(ServerPlayerEntity player, double radius, int durationTicks, int amp) {
        if (radius <= 0) return;
        ServerWorld world = player.getServerWorld();
        Box box = player.getBoundingBox().expand(radius);
        List<Entity> list = world.getOtherEntities(player, box, e -> e instanceof LivingEntity && !(e instanceof net.minecraft.entity.player.PlayerEntity));
        for (Entity e : list) {
            LivingEntity le = (LivingEntity) e;
            boolean undead = le.getType().isIn(EntityTypeTags.UNDEAD);
            RegistryEntry<StatusEffect> eff = undead ? StatusEffects.WITHER : StatusEffects.POISON;
            le.addStatusEffect(new StatusEffectInstance(eff, Math.max(60, durationTicks), Math.max(0, amp)));
        }
    }

    private static void aoeDamageAndDebuff(ServerPlayerEntity player, double radius, double dmg, int durationTicks,
                                           RegistryEntry<StatusEffect> effect, int amp) {
        if (radius <= 0) return;
        ServerWorld world = player.getServerWorld();
        Box box = player.getBoundingBox().expand(radius);
        List<Entity> list = world.getOtherEntities(player, box, e -> e instanceof LivingEntity && !(e instanceof net.minecraft.entity.player.PlayerEntity));
        for (Entity e : list) {
            LivingEntity le = (LivingEntity) e;
            if (dmg > 0) le.damage(player.getDamageSources().playerAttack(player), (float) dmg);
            if (durationTicks > 0) le.addStatusEffect(new StatusEffectInstance(effect, Math.max(40, durationTicks), Math.max(0, amp)));
        }
    }

    private static void aoeDamageAndKnockback(ServerPlayerEntity player, double radius, double dmg, int kbStrength) {
        if (radius <= 0) return;
        ServerWorld world = player.getServerWorld();
        Box box = player.getBoundingBox().expand(radius);
        List<Entity> list = world.getOtherEntities(player, box, e -> e instanceof LivingEntity && !(e instanceof net.minecraft.entity.player.PlayerEntity));
        for (Entity e : list) {
            LivingEntity le = (LivingEntity) e;
            if (dmg > 0) le.damage(player.getDamageSources().playerAttack(player), (float) dmg);

            Vec3d dir = le.getPos().subtract(player.getPos());
            double len = Math.max(0.001, dir.length());
            double dx = dir.x / len;
            double dz = dir.z / len;
            le.takeKnockback(Math.max(0.55f, kbStrength * 0.40f), -dx, -dz);
        }
    }

    private static void applyRandomBuff(ServerPlayerEntity player, int dur) {
        int r = player.getRandom().nextInt(5);
        switch (r) {
            case 0 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, dur, 1, true, false, true));
            case 1 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, dur, 1, true, false, true));
            case 2 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, dur, 1, true, false, true));
            case 3 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, dur, 1, true, false, true));
            default -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, dur, 1, true, false, true));
        }
    }

    private static NbtCompound getCustomData(ItemStack stack) {
        NbtComponent comp = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return comp.copyNbt();
    }

    private static void setCustomData(ItemStack stack, NbtCompound nbt) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
}