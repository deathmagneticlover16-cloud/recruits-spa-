package com.lastpagans.recruitsspa;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Common-side optional integration. No Icy classes are linked directly. */
final class IcysBetterHorsesCompat {
    private static final List<String> ICYS_MOUNTS = List.of(
            "icys-better-horses:morgan_horse", "icys-better-horses:arabian_horse",
            "icys-better-horses:quarter_horse", "icys-better-horses:mustang_horse",
            "icys-better-horses:andalusian_horse", "icys-better-horses:american_paint_horse",
            "icys-better-horses:thoroughbred_horse", "icys-better-horses:appaloosa_horse",
            "icys-better-horses:clydesdale_horse", "icys-better-horses:belgian_horse",
            "icys-better-horses:shire_horse", "icys-better-horses:percheron_horse",
            "icys-better-horses:haflinger_horse", "icys-better-horses:friesian_horse",
            "icys-better-horses:icelandic_horse", "icys-better-horses:horse_cart"
    );
    private static final Map<UUID, Long> LAST_TRAMPLE = new HashMap<>();
    private static final Set<AbstractRecruitEntity> MOUNTED_BLOCKERS = new HashSet<>();
    private static Method horseDataOf;
    private static Method chargeStrike;
    private static Method mayTarget;
    private static Method horseCombatEnabled;
    private static boolean reflectionResolved;

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (!ModList.get().isLoaded("icys_better_horses")) {
            return;
        }
        List<? extends String> configured = RecruitsServerConfig.MountWhiteList.get();
        ArrayList<String> merged = new ArrayList<>(configured);
        if (merged.addAll(ICYS_MOUNTS.stream().filter(id -> !merged.contains(id)).toList())) {
            RecruitsServerConfig.MountWhiteList.set(merged);
            RecruitsServerConfig.SERVER.save();
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof AbstractRecruitEntity recruit)) {
            return;
        }
        if (recruit.level() instanceof ServerLevel level && isMountedBlocking(recruit)) {
            MOUNTED_BLOCKERS.add(recruit);
        }
        if (ModList.get().isLoaded("icys_better_horses") && recruit.level() instanceof ServerLevel level) {
            trample(level, recruit);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MOUNTED_BLOCKERS.forEach(IcysBetterHorsesCompat::keepMountedShieldSpeed);
        MOUNTED_BLOCKERS.clear();
    }

    private static boolean isMountedBlocking(AbstractRecruitEntity recruit) {
        if (!recruit.isPassenger() || !EntityCompat.isHorseLike(recruit.getVehicle()) || !recruit.isUsingItem()) {
            return false;
        }
        ItemStack active = recruit.getUseItem();
        return !active.isEmpty() && active.getItem().canPerformAction(active, ToolActions.SHIELD_BLOCK);
    }

    private static void keepMountedShieldSpeed(AbstractRecruitEntity recruit) {
        if (!isMountedBlocking(recruit)) {
            return;
        }
        var speed = recruit.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getBaseValue() < 0.3D) {
            speed.setBaseValue(0.3D);
        }
    }

    private static void trample(ServerLevel level, AbstractRecruitEntity recruit) {
        if (!(recruit.getVehicle() instanceof AbstractHorse horse) || !resolveReflection() || !combatEnabled()) {
            return;
        }
        double horizontalSpeed = horse.getDeltaMovement().horizontalDistance();
        if (horizontalSpeed < 0.22D || !horse.onGround()) {
            return;
        }

        long now = level.getGameTime();
        var direction = horse.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).normalize();
        var hitbox = horse.getBoundingBox().inflate(0.3D).move(direction.scale(0.5D));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, hitbox)) {
            if (target == horse || target == recruit || horse.hasPassenger(target)
                    || target.isAlliedTo(recruit) || recruit.isAlliedTo(target) || !mayTarget(target)) {
                continue;
            }
            long last = LAST_TRAMPLE.getOrDefault(target.getUUID(), Long.MIN_VALUE / 2);
            if (now - last < 12L) {
                continue;
            }
            try {
                Object horseData = horseDataOf.invoke(null, horse);
                chargeStrike.invoke(null, level, horse, horseData, target);
                LAST_TRAMPLE.put(target.getUUID(), now);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return;
            }
        }
        if ((now & 255L) == 0L) {
            LAST_TRAMPLE.entrySet().removeIf(entry -> now - entry.getValue() > 200L);
        }
    }

    private static synchronized boolean resolveReflection() {
        if (reflectionResolved) {
            return horseDataOf != null;
        }
        reflectionResolved = true;
        try {
            Class<?> horseData = Class.forName("icy.betterhorses.net.IHorseData");
            Class<?> horseCombat = Class.forName("icy.betterhorses.net.feature.HorseCombat");
            Class<?> config = Class.forName("icy.betterhorses.net.BhConfig");
            horseDataOf = horseData.getMethod("of", AbstractHorse.class);
            chargeStrike = horseCombat.getMethod(
                    "chargeStrike", ServerLevel.class, AbstractHorse.class, horseData, LivingEntity.class
            );
            mayTarget = horseCombat.getMethod("mayTarget", LivingEntity.class);
            horseCombatEnabled = config.getMethod("horseCombatEnabled");
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            horseDataOf = null;
            return false;
        }
    }

    private static boolean combatEnabled() {
        try {
            return Boolean.TRUE.equals(horseCombatEnabled.invoke(null));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean mayTarget(LivingEntity target) {
        try {
            return Boolean.TRUE.equals(mayTarget.invoke(null, target));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }
}
