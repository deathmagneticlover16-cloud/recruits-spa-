package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Makes Player Animator layers follow EMF's animation LOD distance setting.
 * EMF only applies that setting to its own animation evaluator, so the bridge
 * mirrors the same distance-based cadence for Recruit Player Animator layers.
 */
final class EmfAnimationLodCompat {
    private static final boolean EMF_LOADED = ModList.get().isLoaded("entity_model_features");
    private static boolean resolved;
    private static Method configMethod;
    private static Method getConfigMethod;
    private static Field lodDistanceField;
    private static Field retainDetailOnLowFpsField;
    private static final Map<AbstractRecruitEntity, Decision> DECISIONS = new WeakHashMap<>();
    private static long configGameTick = Long.MIN_VALUE;
    private static int cachedLodDistance;
    private static boolean cachedRetainDetailOnLowFps;
    private static int cachedFps = 60;

    private EmfAnimationLodCompat() {}

    static boolean isEnabled() {
        return EMF_LOADED && resolve();
    }

    static boolean shouldSkipTick(AbstractRecruitEntity recruit) {
        Decision cached = DECISIONS.get(recruit);
        if (cached != null && cached.tick == recruit.tickCount) {
            return cached.skip;
        }
        boolean skip = calculate(recruit);
        if (cached == null) {
            DECISIONS.put(recruit, new Decision(recruit.tickCount, skip));
        } else {
            cached.tick = recruit.tickCount;
            cached.skip = skip;
        }
        return skip;
    }

    static void clear(AbstractRecruitEntity recruit) {
        DECISIONS.remove(recruit);
    }

    private static boolean calculate(AbstractRecruitEntity recruit) {
        if (!isEnabled()) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            return false;
        }

        try {
            refreshConfig(minecraft);
            if (cachedLodDistance <= 0) {
                return false;
            }

            int factor = (int) (Math.sqrt(minecraft.player.distanceToSqr(recruit)) / cachedLodDistance);
            if (cachedRetainDetailOnLowFps && cachedFps < 60) {
                factor = (int) (factor * (cachedFps / 60.0F));
            }
            float entitySize = Math.max(recruit.getBbWidth(), recruit.getBbHeight());
            if (entitySize > 2.0F) {
                factor = (int) (factor / (entitySize / 2.0F));
            }

            return factor > 0 && Math.floorMod(recruit.tickCount, factor + 1) != 0;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static void refreshConfig(Minecraft minecraft) throws ReflectiveOperationException {
        long gameTick = minecraft.level == null ? Long.MIN_VALUE : minecraft.level.getGameTime();
        if (configGameTick == gameTick) {
            return;
        }
        // Mark the tick before reflective access so a broken optional API cannot
        // throw once for every visible Recruit in the same frame.
        configGameTick = gameTick;
        cachedLodDistance = 0;
        Object handler = configMethod.invoke(null);
        Object config = getConfigMethod.invoke(handler);
        cachedLodDistance = lodDistanceField.getInt(config);
        cachedRetainDetailOnLowFps = retainDetailOnLowFpsField.getBoolean(config);
        cachedFps = minecraft.getFps();
    }

    private static final class Decision {
        private int tick;
        private boolean skip;

        private Decision(int tick, boolean skip) {
            this.tick = tick;
            this.skip = skip;
        }
    }

    private static boolean resolve() {
        if (resolved) {
            return configMethod != null;
        }
        synchronized (EmfAnimationLodCompat.class) {
            if (resolved) {
                return configMethod != null;
            }
            resolved = true;
            try {
                Class<?> emf = Class.forName("traben.entity_model_features.EMF");
                configMethod = emf.getMethod("config");
                Class<?> handlerClass = configMethod.getReturnType();
                getConfigMethod = handlerClass.getMethod("getConfig");
                Class<?> configClass = Class.forName("traben.entity_model_features.config.EMFConfig");
                lodDistanceField = configClass.getField("animationLODDistance");
                retainDetailOnLowFpsField = configClass.getField("retainDetailOnLowFps");
                return true;
            } catch (ReflectiveOperationException | LinkageError ignored) {
                configMethod = null;
                return false;
            }
        }
    }
}
