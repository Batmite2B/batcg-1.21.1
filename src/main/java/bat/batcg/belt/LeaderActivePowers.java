package bat.batcg.belt;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class LeaderActivePowers {

    private LeaderActivePowers() {}

    // NBT keys inside belt CUSTOM_DATA
    private static final String KEY_ACTIVE_TYPE   = "batcg_active_power";      // "FIRE_BREATH" / "BUBBLE_BEAM" / "MAGNET"
    private static final String KEY_ACTIVE_UNTIL  = "batcg_active_until";      // world time tick when ends
    private static final String KEY_ACTIVE_LAST   = "batcg_active_last";       // last DAMAGE tick executed (throttle)
    private static final String KEY_RANGE         = "batcg_active_range";
    private static final String KEY_DMG           = "batcg_active_dmg";
    private static final String KEY_DOT           = "batcg_active_dot";        // dotMin for cone
    private static final String KEY_FIRE_TICKS    = "batcg_active_fire_ticks"; // fire duration each hit tick
    private static final String KEY_DEBUFF_TICKS  = "batcg_active_debuff";     // debuff duration
    private static final String KEY_AMP           = "batcg_active_amp";        // amplifier

    /** Call this once per tick for each player (server-side). */
    public static void tick(ServerPlayerEntity player) {
        ItemStack belt = BatcgBeltApi.getEquippedBelt(player);
        if (belt == null || belt.isEmpty()) return;

        NbtCompound tag = getCustomData(belt);
        String type = tag.getString(KEY_ACTIVE_TYPE);
        if (type == null || type.isEmpty()) return;

        long now = player.getWorld().getTime();
        long until = tag.getLong(KEY_ACTIVE_UNTIL);

        if (now >= until) {
            clear(tag);
            setCustomData(belt, tag);
            return;
        }

        // MAGNET: se siente mejor cada tick (pull continuo)
        if (type.equals("MAGNET")) {
            magnetTick(player, tag);
            return;
        }

        // Para FIRE/BUBBLE:
        // - Partículas: cada tick (así no se "cortan")
        // - Daño: cada 4 ticks (así no spamea demasiado)
        long lastDmg = tag.getLong(KEY_ACTIVE_LAST);
        boolean doDamage = (now - lastDmg) >= 4;

        if (doDamage) {
            tag.putLong(KEY_ACTIVE_LAST, now);
            setCustomData(belt, tag); // guardar throttle
        }

        switch (type) {
            case "FIRE_BREATH" -> fireBreathTick(player, tag, doDamage);
            case "BUBBLE_BEAM" -> bubbleBeamTick(player, tag, doDamage);
        }
    }

    public static void startFireBreath(ItemStack belt, ServerPlayerEntity player, double range, double dmg, double dotMin, int fireTicks, int durationTicks) {
        long now = player.getWorld().getTime();
        NbtCompound tag = getCustomData(belt);
        tag.putString(KEY_ACTIVE_TYPE, "FIRE_BREATH");
        tag.putLong(KEY_ACTIVE_UNTIL, now + Math.max(20, durationTicks));
        tag.putLong(KEY_ACTIVE_LAST, now - 99);
        tag.putDouble(KEY_RANGE, range);
        tag.putDouble(KEY_DMG, dmg);
        tag.putDouble(KEY_DOT, dotMin);
        tag.putInt(KEY_FIRE_TICKS, Math.max(20, fireTicks));
        setCustomData(belt, tag);
    }

    public static void startBubbleBeam(ItemStack belt, ServerPlayerEntity player, double range, double dmg, double dotMin, int debuffTicks, int amp, int durationTicks) {
        long now = player.getWorld().getTime();
        NbtCompound tag = getCustomData(belt);
        tag.putString(KEY_ACTIVE_TYPE, "BUBBLE_BEAM");
        tag.putLong(KEY_ACTIVE_UNTIL, now + Math.max(20, durationTicks));
        tag.putLong(KEY_ACTIVE_LAST, now - 99);
        tag.putDouble(KEY_RANGE, range);
        tag.putDouble(KEY_DMG, dmg);
        tag.putDouble(KEY_DOT, dotMin);
        tag.putInt(KEY_DEBUFF_TICKS, Math.max(20, debuffTicks));
        tag.putInt(KEY_AMP, Math.max(0, amp));
        setCustomData(belt, tag);
    }

    public static void startMagnet(ItemStack belt, ServerPlayerEntity player, double radius, int durationTicks) {
        long now = player.getWorld().getTime();
        NbtCompound tag = getCustomData(belt);
        tag.putString(KEY_ACTIVE_TYPE, "MAGNET");
        tag.putLong(KEY_ACTIVE_UNTIL, now + Math.max(20, durationTicks));
        tag.putLong(KEY_ACTIVE_LAST, now - 99);
        tag.putDouble(KEY_RANGE, Math.max(2.0, radius));
        setCustomData(belt, tag);
    }

    // ---------- ticks ----------

    private static void fireBreathTick(ServerPlayerEntity player, NbtCompound tag, boolean doDamage) {
        ServerWorld world = player.getServerWorld();
        double range = tag.getDouble(KEY_RANGE);
        double dmg = tag.getDouble(KEY_DMG);
        double dotMin = tag.getDouble(KEY_DOT);
        int fireTicks = tag.getInt(KEY_FIRE_TICKS);

        Vec3d look = player.getRotationVec(1.0f).normalize();

        // Partículas MÁS “persistentes”:
        // - FLAME (corto) + SMOKE (medio) + CAMPFIRE_COSY_SMOKE (largo) para que se vea continuo
        for (double t = 0.8; t <= range; t += 0.40) {
            Vec3d ppos = player.getPos().add(0, 1.25, 0).add(look.multiply(t));

            // flame visible
            world.spawnParticles(ParticleTypes.FLAME, ppos.x, ppos.y, ppos.z,
                    2, 0.18, 0.10, 0.18, 0.005);

            // smoke base
            world.spawnParticles(ParticleTypes.SMOKE, ppos.x, ppos.y, ppos.z,
                    1, 0.14, 0.08, 0.14, 0.002);

            // humo "duradero" (no siempre, para no saturar)
            if (player.getRandom().nextInt(3) == 0) {
                world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, ppos.x, ppos.y, ppos.z,
                        1, 0.16, 0.10, 0.16, 0.001);
            }
        }

        if (!doDamage) return;

        Box box = player.getBoundingBox().expand(range);
        List<Entity> list = world.getOtherEntities(player, box, e -> e instanceof LivingEntity && !(e instanceof net.minecraft.entity.player.PlayerEntity));
        for (Entity e : list) {
            LivingEntity le = (LivingEntity) e;

            Vec3d to = le.getPos().subtract(player.getPos());
            double dist = to.length();
            if (dist < 0.001 || dist > range) continue;

            Vec3d dir = to.normalize();
            if (look.dotProduct(dir) < dotMin) continue;

            le.damage(player.getDamageSources().playerAttack(player), (float) dmg);
            le.setOnFireFor(Math.max(1, fireTicks / 20));
        }
    }

    private static void bubbleBeamTick(ServerPlayerEntity player, NbtCompound tag, boolean doDamage) {
        ServerWorld world = player.getServerWorld();
        double range = tag.getDouble(KEY_RANGE);
        double dmg = tag.getDouble(KEY_DMG);
        double dotMin = tag.getDouble(KEY_DOT);
        int debuffTicks = tag.getInt(KEY_DEBUFF_TICKS);
        int amp = tag.getInt(KEY_AMP);

        Vec3d look = player.getRotationVec(1.0f).normalize();

        // Partículas con mejor "persistencia":
        // BUBBLE_COLUMN_UP/UNDERWATER tienden a verse más continuas que solo BUBBLE.
        // Partículas más naturales: stream continuo
        for (double t = 0.8; t <= range; t += 0.35) {
            Vec3d ppos = player.getPos().add(0, 1.25, 0).add(look.multiply(t));

            // "columna" que dura más y se siente como flujo
            world.spawnParticles(ParticleTypes.BUBBLE_COLUMN_UP, ppos.x, ppos.y, ppos.z,
                    2, 0.12, 0.10, 0.12, 0.01);

            // gotitas de agua para que parezca chorro
            world.spawnParticles(ParticleTypes.FALLING_WATER, ppos.x, ppos.y, ppos.z,
                    1, 0.10, 0.10, 0.10, 0.01);

            // neblina suave para “rellenar” visualmente y quitar el efecto pop
            world.spawnParticles(ParticleTypes.CLOUD, ppos.x, ppos.y, ppos.z,
                    1, 0.08, 0.08, 0.08, 0.003);
        }

// un poco de splash en la punta para que se sienta impactante
        Vec3d tip = player.getPos().add(0, 1.25, 0).add(look.multiply(range));
        world.spawnParticles(ParticleTypes.SPLASH, tip.x, tip.y, tip.z,
                4, 0.20, 0.10, 0.20, 0.02);

        if (!doDamage) return;

        Box box = player.getBoundingBox().expand(range);
        List<Entity> list = world.getOtherEntities(player, box, e -> e instanceof LivingEntity && !(e instanceof net.minecraft.entity.player.PlayerEntity));
        for (Entity e : list) {
            LivingEntity le = (LivingEntity) e;

            Vec3d to = le.getPos().subtract(player.getPos());
            double dist = to.length();
            if (dist < 0.001 || dist > range) continue;

            Vec3d dir = to.normalize();
            if (look.dotProduct(dir) < dotMin) continue;

            le.damage(player.getDamageSources().playerAttack(player), (float) dmg);
            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, Math.max(20, debuffTicks), Math.max(0, amp)));
        }
    }

    private static void magnetTick(ServerPlayerEntity player, NbtCompound tag) {
        ServerWorld world = player.getServerWorld();
        double radius = tag.getDouble(KEY_RANGE);

        Box box = player.getBoundingBox().expand(radius);

        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, box, it -> true);
        for (ItemEntity it : items) {
            Vec3d dir = player.getPos().add(0, 0.4, 0).subtract(it.getPos());
            double len = Math.max(0.001, dir.length());

            Vec3d v = dir.multiply(0.55 / len);
            it.addVelocity(v.x, v.y, v.z);

            if (len < 3.2) {
                it.updatePosition(player.getX(), player.getY() + 0.25, player.getZ());
            }
        }

        List<ExperienceOrbEntity> orbs = world.getEntitiesByClass(ExperienceOrbEntity.class, box, o -> true);
        for (ExperienceOrbEntity o : orbs) {
            Vec3d dir = player.getPos().add(0, 0.4, 0).subtract(o.getPos());
            double len = Math.max(0.001, dir.length());
            Vec3d v = dir.multiply(0.50 / len);
            o.addVelocity(v.x, v.y, v.z);

            if (len < 3.2) {
                o.updatePosition(player.getX(), player.getY() + 0.25, player.getZ());
            }
        }

        world.spawnParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(),
                6, 0.35, 0.35, 0.35, 0.01);
    }

    private static void clear(NbtCompound tag) {
        tag.remove(KEY_ACTIVE_TYPE);
        tag.remove(KEY_ACTIVE_UNTIL);
        tag.remove(KEY_ACTIVE_LAST);
        tag.remove(KEY_RANGE);
        tag.remove(KEY_DMG);
        tag.remove(KEY_DOT);
        tag.remove(KEY_FIRE_TICKS);
        tag.remove(KEY_DEBUFF_TICKS);
        tag.remove(KEY_AMP);
    }

    private static NbtCompound getCustomData(ItemStack stack) {
        NbtComponent comp = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return comp.copyNbt();
    }

    private static void setCustomData(ItemStack stack, NbtCompound nbt) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
}