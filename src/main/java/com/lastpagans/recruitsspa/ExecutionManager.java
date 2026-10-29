package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Server-authoritative matched-combat controller.
 *
 * 0.5 adds execution types, Better Combat-aligned timing and semantic SFX cues.
 * Eligible executions remain deterministic (100%) while the library is being
 * tested; chance/configuration can come after the choreography is stable.
 */
public final class ExecutionManager {
    static final ExecutionManager INSTANCE = new ExecutionManager();

    private static final double MAX_VERTICAL_GAP = 0.75D;
    private static final ClassValue<NoAiAccess> NO_AI_ACCESS = new ClassValue<>() {
        @Override
        protected NoAiAccess computeValue(Class<?> type) {
            return new NoAiAccess(
                    findMethod(type, new String[]{"m_21525_", "isNoAi"}),
                    findMethod(type, new String[]{"m_21557_", "setNoAi"}, boolean.class)
            );
        }
    };

    private final Map<Integer, ExecutionSession> byEntityId = new HashMap<>();
    private final List<ExecutionSession> sessions = new ArrayList<>();
    private final Map<AbstractRecruitEntity, Integer> lastSwordExecutionByAttacker = new WeakHashMap<>();

    private ExecutionManager() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onFinalDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target instanceof AbstractRecruitEntity victim)) {
            return;
        }
        if (victim.level() == null || victim.level().isClientSide()) {
            return;
        }

        int victimId = victim.getId();
        if (byEntityId.containsKey(victimId)) {
            event.setCanceled(true);
            return;
        }

        DamageSource source = event.getSource();
        if (source == null) {
            return;
        }
        Entity causing = source.getEntity();
        if (!(causing instanceof AbstractRecruitEntity attacker) || attacker == victim) {
            return;
        }

        int attackerId = attacker.getId();
        if (byEntityId.containsKey(attackerId)) {
            event.setCanceled(true);
            return;
        }

        if (event.getAmount() + 0.0001F < victim.getHealth()) {
            return;
        }

        ExecutionSpec spec = chooseExecution(attacker, victim);
        if (spec == null || !eligibleGeometry(attacker, victim, spec.maxStartDistance)) {
            return;
        }

        event.setCanceled(true);
        start(attacker, victim, source, spec);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || sessions.isEmpty()) {
            return;
        }

        Iterator<ExecutionSession> iterator = sessions.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick()) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        sessions.clear();
        byEntityId.clear();
        lastSwordExecutionByAttacker.clear();
    }

    private void start(AbstractRecruitEntity attacker, AbstractRecruitEntity victim, DamageSource source, ExecutionSpec spec) {
        Vec3 a = attacker.position();
        Vec3 v = victim.position();
        double dx = v.x - a.x;
        double dz = v.z - a.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.001D) {
            return;
        }

        double ux = dx / horizontal;
        double uz = dz / horizontal;
        double midX = (a.x + v.x) * 0.5D;
        double midZ = (a.z + v.z) * 0.5D;
        double half = spec.matchDistance * 0.5D;

        double attackerX = midX - ux * half;
        double attackerZ = midZ - uz * half;
        double victimX = midX + ux * half;
        double victimZ = midZ + uz * half;

        float attackerYaw = yawToward(attackerX, attackerZ, victimX, victimZ);
        float victimYaw = wrapDegrees(attackerYaw + 180.0F);

        ExecutionSession session = new ExecutionSession(
                attacker,
                victim,
                source,
                spec,
                attackerX,
                a.y,
                attackerZ,
                victimX,
                v.y,
                victimZ,
                attackerYaw,
                victimYaw,
                readNoAi(attacker),
                readNoAi(victim)
        );

        sessions.add(session);
        byEntityId.put(attacker.getId(), session);
        byEntityId.put(victim.getId(), session);

        session.lockBoth();
        send(attacker, attacker.getId(), victim.getId(), ExecutionSyncMessage.START, spec.type);
    }

    private ExecutionSpec chooseExecution(AbstractRecruitEntity attacker, AbstractRecruitEntity victim) {
        WeaponKind attackerWeapon = weaponKind(attacker.getMainHandItem());
        WeaponKind victimWeapon = weaponKind(victim.getMainHandItem());

        if (attackerWeapon == WeaponKind.SWORD && victimWeapon == WeaponKind.SWORD) {
            // V0.6 testing weights: enough long/matched encounters to be visible in battle
            // footage, while quick finishes still remain the majority. Reroll once when
            // the same attacker would immediately repeat the same execution.
            ExecutionSpec picked = randomSwordExecution();
            Integer last = lastSwordExecutionByAttacker.get(attacker);
            if (last != null && last == picked.type) {
                ExecutionSpec reroll = randomSwordExecution();
                if (reroll.type != last) {
                    picked = reroll;
                }
            }
            lastSwordExecutionByAttacker.put(attacker, picked.type);
            return picked;
        }

        if (attackerWeapon == WeaponKind.SPEAR) {
            return ExecutionSpec.SPEAR_CHEST;
        }

        return null;
    }

    private static ExecutionSpec randomSwordExecution() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 25) return ExecutionSpec.SWORD_STAB;
        if (roll < 45) return ExecutionSpec.SWORD_SLASH;
        if (roll < 63) return ExecutionSpec.SWORD_UPPERCUT;
        if (roll < 88) return ExecutionSpec.SWORD_MATCHED;
        return ExecutionSpec.SWORD_DUEL;
    }

    private boolean eligibleGeometry(AbstractRecruitEntity attacker, AbstractRecruitEntity victim, double maxStartDistance) {
        if (attacker.level() != victim.level()) {
            return false;
        }
        if (attacker.isPassenger() || victim.isPassenger()) {
            return false;
        }
        if (!attacker.onGround() || !victim.onGround()) {
            return false;
        }
        if (attacker.isInWater() || victim.isInWater() || attacker.isFallFlying() || victim.isFallFlying()) {
            return false;
        }
        if (attacker.getHealth() <= 0.0F || victim.getHealth() <= 0.0F) {
            return false;
        }

        Vec3 a = attacker.position();
        Vec3 v = victim.position();
        double dy = Math.abs(v.y - a.y);
        if (dy > MAX_VERTICAL_GAP) {
            return false;
        }
        double dx = v.x - a.x;
        double dz = v.z - a.z;
        double horizontalSq = dx * dx + dz * dz;
        return horizontalSq >= 0.16D && horizontalSq <= maxStartDistance * maxStartDistance;
    }

    private static WeaponKind weaponKind(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return WeaponKind.OTHER;
        }

        Item item = stack.getItem();
        if (item == null) {
            return WeaponKind.OTHER;
        }

        if (item instanceof SwordItem) {
            return WeaponKind.SWORD;
        }

        String use = stack.getUseAnimation() == null ? "" : stack.getUseAnimation().name();
        String name = item.getClass().getSimpleName().toLowerCase();

        if ("SPEAR".equals(use)
                || name.contains("spear")
                || name.contains("pike")
                || name.contains("lance")
                || name.contains("javelin")) {
            return WeaponKind.SPEAR;
        }

        if (name.contains("sword") || name.contains("sabre") || name.contains("saber")) {
            return WeaponKind.SWORD;
        }

        return WeaponKind.OTHER;
    }

    private static float yawToward(double fromX, double fromZ, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        return wrapDegrees((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D));
    }

    private static float wrapDegrees(float value) {
        value %= 360.0F;
        if (value >= 180.0F) value -= 360.0F;
        if (value < -180.0F) value += 360.0F;
        return value;
    }

    private static void send(AbstractRecruitEntity trackedRecruit, int attackerId, int victimId, int phase, int type) {
        RecruitsSeriousAnimations.EXECUTION_CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY.with(() -> trackedRecruit),
                new ExecutionSyncMessage(attackerId, victimId, phase, type)
        );
    }

    private static boolean readNoAi(AbstractRecruitEntity recruit) {
        Method getter = NO_AI_ACCESS.get(recruit.getClass()).getter;
        if (getter == null) return false;
        try {
            return Boolean.TRUE.equals(getter.invoke(recruit));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static void setNoAi(AbstractRecruitEntity recruit, boolean value) {
        Method setter = NO_AI_ACCESS.get(recruit.getClass()).setter;
        if (setter == null) return;
        try {
            setter.invoke(recruit, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Method findMethod(Class<?> type, String[] names, Class<?>... parameterTypes) {
        for (String name : names) {
            try {
                return type.getMethod(name, parameterTypes);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private record NoAiAccess(Method getter, Method setter) {}

    private enum WeaponKind {
        SWORD,
        SPEAR,
        OTHER
    }

    private enum ExecutionSpec {
        // Better Combat one_handed_stab.
        SWORD_STAB(
                ExecutionTypes.SWORD_STAB_01,
                1.40D, 2.8D,
                11, 30, 46,
                new int[]{8}, 11, 12, 18, 29
        ),

        // Better Combat right-to-left one-handed horizontal cut.
        SWORD_SLASH(
                ExecutionTypes.SWORD_SLASH_02,
                1.48D, 2.9D,
                11, 30, 46,
                new int[]{8}, -1, 11, 17, 29
        ),

        // Better Combat rising counter. Short and violent, with a different victim fall.
        SWORD_UPPERCUT(
                ExecutionTypes.SWORD_UPPERCUT_03,
                1.42D, 2.8D,
                11, 30, 48,
                new int[]{8}, -1, 11, 18, 31
        ),

        // Medium matched exchange: guard, clash, return cut, parry, finishing thrust.
        SWORD_MATCHED(
                ExecutionTypes.SWORD_MATCHED_04,
                1.46D, 3.0D,
                39, 58, 70,
                new int[]{13, 27}, 39, 40, 46, 60
        ),

        // Longer cinematic duel intended to remain readable from an RTS/camera view.
        // Three exchanges precede the decisive thrust.
        SWORD_DUEL(
                ExecutionTypes.SWORD_DUEL_05,
                1.50D, 3.1D,
                68, 88, 104,
                new int[]{15, 32, 49}, 68, 69, 76, 92
        ),

        SPEAR_CHEST(
                ExecutionTypes.SPEAR_CHEST_01,
                1.85D, 3.2D,
                11, 30, 48,
                new int[]{}, 11, 12, 18, 31
        );

        final int type;
        final double matchDistance;
        final double maxStartDistance;
        final int killTick;
        final int releaseAttackerTick;
        final int endTick;
        final int[] clashTicks;
        final int stabTick;
        final int painTick;
        final int deathTick;
        final int bodyFallTick;

        ExecutionSpec(
                int type,
                double matchDistance,
                double maxStartDistance,
                int killTick,
                int releaseAttackerTick,
                int endTick,
                int[] clashTicks,
                int stabTick,
                int painTick,
                int deathTick,
                int bodyFallTick
        ) {
            this.type = type;
            this.matchDistance = matchDistance;
            this.maxStartDistance = maxStartDistance;
            this.killTick = killTick;
            this.releaseAttackerTick = releaseAttackerTick;
            this.endTick = endTick;
            this.clashTicks = clashTicks;
            this.stabTick = stabTick;
            this.painTick = painTick;
            this.deathTick = deathTick;
            this.bodyFallTick = bodyFallTick;
        }
    }

    private final class ExecutionSession {
        final AbstractRecruitEntity attacker;
        final AbstractRecruitEntity victim;
        final DamageSource source;
        final ExecutionSpec spec;
        final int attackerId;
        final int victimId;
        final double attackerX, attackerY, attackerZ;
        final double victimX, victimY, victimZ;
        final float attackerYaw, victimYaw;
        final boolean attackerNoAiBefore;
        final boolean victimNoAiBefore;

        int age;
        boolean killed;
        boolean attackerReleased;

        ExecutionSession(
                AbstractRecruitEntity attacker,
                AbstractRecruitEntity victim,
                DamageSource source,
                ExecutionSpec spec,
                double attackerX,
                double attackerY,
                double attackerZ,
                double victimX,
                double victimY,
                double victimZ,
                float attackerYaw,
                float victimYaw,
                boolean attackerNoAiBefore,
                boolean victimNoAiBefore
        ) {
            this.attacker = attacker;
            this.victim = victim;
            this.source = source;
            this.spec = spec;
            this.attackerId = attacker.getId();
            this.victimId = victim.getId();
            this.attackerX = attackerX;
            this.attackerY = attackerY;
            this.attackerZ = attackerZ;
            this.victimX = victimX;
            this.victimY = victimY;
            this.victimZ = victimZ;
            this.attackerYaw = attackerYaw;
            this.victimYaw = victimYaw;
            this.attackerNoAiBefore = attackerNoAiBefore;
            this.victimNoAiBefore = victimNoAiBefore;
        }

        boolean tick() {
            age++;

            if (!attackerReleased && attacker.getHealth() <= 0.0F) {
                return abort();
            }
            if (!killed && victim.getHealth() <= 0.0F) {
                return abort();
            }

            if (!attackerReleased) {
                lock(attacker, attackerX, attackerY, attackerZ, attackerYaw);
            }
            if (!killed) {
                lock(victim, victimX, victimY, victimZ, victimYaw);
            }

            fireCues();

            if (!killed && age >= spec.killTick) {
                killed = true;
                victim.setHealth(0.0F);
                victim.die(source);
            }

            if (!attackerReleased && age >= spec.releaseAttackerTick) {
                attackerReleased = true;
                setNoAi(attacker, attackerNoAiBefore);
                byEntityId.remove(attackerId);
                send(attacker, attackerId, victimId, ExecutionSyncMessage.RELEASE_ATTACKER, spec.type);
            }

            if (age >= spec.endTick) {
                setNoAi(victim, victimNoAiBefore);
                byEntityId.remove(attackerId);
                byEntityId.remove(victimId);
                send(attacker, attackerId, victimId, ExecutionSyncMessage.END, spec.type);
                return true;
            }

            return false;
        }

        private void fireCues() {
            for (int clashTick : spec.clashTicks) {
                if (age == clashTick) {
                    send(attacker, attackerId, victimId, ExecutionSyncMessage.SFX_CLASH, spec.type);
                    break;
                }
            }
            if (age == spec.stabTick) {
                send(attacker, attackerId, victimId, ExecutionSyncMessage.SFX_STAB, spec.type);
            }
            if (age == spec.painTick) {
                send(attacker, attackerId, victimId, ExecutionSyncMessage.SFX_PAIN, spec.type);
            }
            if (age == spec.deathTick) {
                send(attacker, attackerId, victimId, ExecutionSyncMessage.SFX_DEATH, spec.type);
            }
            if (age == spec.bodyFallTick) {
                send(attacker, attackerId, victimId, ExecutionSyncMessage.SFX_BODY_FALL, spec.type);
            }
        }

        void lockBoth() {
            setNoAi(attacker, true);
            setNoAi(victim, true);
            lock(attacker, attackerX, attackerY, attackerZ, attackerYaw);
            lock(victim, victimX, victimY, victimZ, victimYaw);
        }

        private boolean abort() {
            setNoAi(attacker, attackerNoAiBefore);
            setNoAi(victim, victimNoAiBefore);
            byEntityId.remove(attackerId);
            byEntityId.remove(victimId);
            send(attacker, attackerId, victimId, ExecutionSyncMessage.END, spec.type);
            return true;
        }
    }

    private static void lock(AbstractRecruitEntity recruit, double x, double y, double z, float yaw) {
        try {
            recruit.getNavigation().stop();
        } catch (Throwable ignored) {
        }
        recruit.setDeltaMovement(Vec3.ZERO);
        recruit.setPos(x, y, z);
        recruit.setYRot(yaw);
        recruit.yBodyRot = yaw;
        recruit.yHeadRot = yaw;
    }
}
