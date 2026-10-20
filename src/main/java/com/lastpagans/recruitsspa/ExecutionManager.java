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
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final Map<Integer, ExecutionSession> byEntityId = new HashMap<>();
    private final List<ExecutionSession> sessions = new ArrayList<>();
    private final Map<Integer, Integer> lastSwordExecutionByAttacker = new HashMap<>();

    private ExecutionManager() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onFinalDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target instanceof AbstractRecruitEntity victim)) {
            return;
        }
        if (victim.m_20193_() == null || victim.m_20193_().m_5776_()) {
            return;
        }

        int victimId = victim.m_19879_();
        if (byEntityId.containsKey(victimId)) {
            event.setCanceled(true);
            return;
        }

        DamageSource source = event.getSource();
        if (source == null) {
            return;
        }
        Entity causing = source.m_7639_();
        if (!(causing instanceof AbstractRecruitEntity attacker) || attacker == victim) {
            return;
        }

        int attackerId = attacker.m_19879_();
        if (byEntityId.containsKey(attackerId)) {
            event.setCanceled(true);
            return;
        }

        if (event.getAmount() + 0.0001F < victim.m_21223_()) {
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

        List<ExecutionSession> finished = null;
        for (ExecutionSession session : new ArrayList<>(sessions)) {
            if (session.tick()) {
                if (finished == null) {
                    finished = new ArrayList<>();
                }
                finished.add(session);
            }
        }

        if (finished != null) {
            sessions.removeAll(finished);
        }
    }

    private void start(AbstractRecruitEntity attacker, AbstractRecruitEntity victim, DamageSource source, ExecutionSpec spec) {
        Vec3 a = attacker.m_20182_();
        Vec3 v = victim.m_20182_();
        double dx = v.f_82479_ - a.f_82479_;
        double dz = v.f_82481_ - a.f_82481_;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.001D) {
            return;
        }

        double ux = dx / horizontal;
        double uz = dz / horizontal;
        double midX = (a.f_82479_ + v.f_82479_) * 0.5D;
        double midZ = (a.f_82481_ + v.f_82481_) * 0.5D;
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
                a.f_82480_,
                attackerZ,
                victimX,
                v.f_82480_,
                victimZ,
                attackerYaw,
                victimYaw,
                readNoAi(attacker),
                readNoAi(victim)
        );

        sessions.add(session);
        byEntityId.put(attacker.m_19879_(), session);
        byEntityId.put(victim.m_19879_(), session);

        session.lockBoth();
        send(attacker.m_19879_(), victim.m_19879_(), ExecutionSyncMessage.START, spec.type);
    }

    private ExecutionSpec chooseExecution(AbstractRecruitEntity attacker, AbstractRecruitEntity victim) {
        WeaponKind attackerWeapon = weaponKind(attacker.m_21205_());
        WeaponKind victimWeapon = weaponKind(victim.m_21205_());

        if (attackerWeapon == WeaponKind.SWORD && victimWeapon == WeaponKind.SWORD) {
            // V0.6 testing weights: enough long/matched encounters to be visible in battle
            // footage, while quick finishes still remain the majority. Reroll once when
            // the same attacker would immediately repeat the same execution.
            ExecutionSpec picked = randomSwordExecution();
            Integer last = lastSwordExecutionByAttacker.get(attacker.m_19879_());
            if (last != null && last == picked.type) {
                ExecutionSpec reroll = randomSwordExecution();
                if (reroll.type != last) {
                    picked = reroll;
                }
            }
            lastSwordExecutionByAttacker.put(attacker.m_19879_(), picked.type);
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
        if (attacker.m_20193_() != victim.m_20193_()) {
            return false;
        }
        if (attacker.m_20159_() || victim.m_20159_()) {
            return false;
        }
        if (!attacker.m_20096_() || !victim.m_20096_()) {
            return false;
        }
        if (attacker.m_20072_() || victim.m_20072_() || attacker.m_6069_() || victim.m_6069_()) {
            return false;
        }
        if (attacker.m_21223_() <= 0.0F || victim.m_21223_() <= 0.0F) {
            return false;
        }

        Vec3 a = attacker.m_20182_();
        Vec3 v = victim.m_20182_();
        double dy = Math.abs(v.f_82480_ - a.f_82480_);
        if (dy > MAX_VERTICAL_GAP) {
            return false;
        }
        double dx = v.f_82479_ - a.f_82479_;
        double dz = v.f_82481_ - a.f_82481_;
        double horizontalSq = dx * dx + dz * dz;
        return horizontalSq >= 0.16D && horizontalSq <= maxStartDistance * maxStartDistance;
    }

    private static WeaponKind weaponKind(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return WeaponKind.OTHER;
        }

        Item item = stack.m_41720_();
        if (item == null) {
            return WeaponKind.OTHER;
        }

        if (item instanceof SwordItem) {
            return WeaponKind.SWORD;
        }

        String use = stack.m_41780_() == null ? "" : stack.m_41780_().name();
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

    private static void send(int attackerId, int victimId, int phase, int type) {
        RecruitsSeriousAnimations.EXECUTION_CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                new ExecutionSyncMessage(attackerId, victimId, phase, type)
        );
    }

    private static boolean readNoAi(AbstractRecruitEntity recruit) {
        Object value = invokeNoArg(recruit, "m_21525_", "isNoAi");
        return value instanceof Boolean b && b;
    }

    private static void setNoAi(AbstractRecruitEntity recruit, boolean value) {
        invokeBoolean(recruit, value, "m_21557_", "setNoAi");
    }

    private static Object invokeNoArg(Object object, String... names) {
        for (String name : names) {
            try {
                Method method = object.getClass().getMethod(name);
                return method.invoke(object);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static void invokeBoolean(Object object, boolean value, String... names) {
        for (String name : names) {
            try {
                Method method = object.getClass().getMethod(name, boolean.class);
                method.invoke(object, value);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

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
            this.attackerId = attacker.m_19879_();
            this.victimId = victim.m_19879_();
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

            if (!attackerReleased && attacker.m_21223_() <= 0.0F) {
                return abort();
            }
            if (!killed && victim.m_21223_() <= 0.0F) {
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
                victim.m_21153_(0.0F);
                victim.m_6667_(source);
            }

            if (!attackerReleased && age >= spec.releaseAttackerTick) {
                attackerReleased = true;
                setNoAi(attacker, attackerNoAiBefore);
                byEntityId.remove(attackerId);
                send(attackerId, victimId, ExecutionSyncMessage.RELEASE_ATTACKER, spec.type);
            }

            if (age >= spec.endTick) {
                setNoAi(victim, victimNoAiBefore);
                byEntityId.remove(attackerId);
                byEntityId.remove(victimId);
                send(attackerId, victimId, ExecutionSyncMessage.END, spec.type);
                return true;
            }

            return false;
        }

        private void fireCues() {
            for (int clashTick : spec.clashTicks) {
                if (age == clashTick) {
                    send(attackerId, victimId, ExecutionSyncMessage.SFX_CLASH, spec.type);
                    break;
                }
            }
            if (age == spec.stabTick) {
                send(attackerId, victimId, ExecutionSyncMessage.SFX_STAB, spec.type);
            }
            if (age == spec.painTick) {
                send(attackerId, victimId, ExecutionSyncMessage.SFX_PAIN, spec.type);
            }
            if (age == spec.deathTick) {
                send(attackerId, victimId, ExecutionSyncMessage.SFX_DEATH, spec.type);
            }
            if (age == spec.bodyFallTick) {
                send(attackerId, victimId, ExecutionSyncMessage.SFX_BODY_FALL, spec.type);
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
            send(attackerId, victimId, ExecutionSyncMessage.END, spec.type);
            return true;
        }
    }

    private static void lock(AbstractRecruitEntity recruit, double x, double y, double z, float yaw) {
        try {
            recruit.m_21573_().m_26573_();
        } catch (Throwable ignored) {
        }
        recruit.m_20256_(new Vec3(0.0D, 0.0D, 0.0D));
        recruit.m_6034_(x, y, z);
        recruit.m_146922_(yaw);
        recruit.f_20883_ = yaw;
        recruit.f_20885_ = yaw;
    }
}
