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
    private static boolean resolved;
    private static Method configMethod;
    private static Method getConfigMethod;
    private static Field lodDistanceField;
    private static Field retainDetailOnLowFpsField;
    private static final Map<AbstractRecruitEntity, Decision> DECISIONS = new WeakHashMap<>();

    private EmfAnimationLodCompat() {}

    static boolean shouldSkipTick(AbstractRecruitEntity recruit) {
        Decision cached = DECISIONS.get(recruit);
        if (cached != null && cached.tick == recruit.tickCount) {
            return cached.skip;
        }
        boolean skip = calculate(recruit);
        DECISIONS.put(recruit, new Decision(recruit.tickCount, skip));
        return skip;
    }

    private static boolean calculate(AbstractRecruitEntity recruit) {
        if (!ModList.get().isLoaded("entity_model_features") || !resolve()) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            return false;
        }

        try {
            Object handler = configMethod.invoke(null);
            Object config = getConfigMethod.invoke(handler);
            int lodDistance = lodDistanceField.getInt(config);
            if (lodDistance <= 0) {
                return false;
            }

            int factor = (int) (Math.sqrt(minecraft.player.distanceToSqr(recruit)) / lodDistance);
            if (retainDetailOnLowFpsField.getBoolean(config) && minecraft.getFps() < 60) {
                factor = (int) (factor * (minecraft.getFps() / 60.0F));
            }
            if (Math.max(recruit.getBbWidth(), recruit.getBbHeight()) > 2.0F) {
                factor = (int) (factor / (Math.max(recruit.getBbWidth(), recruit.getBbHeight()) / 2.0F));
            }

            return factor > 0 && Math.floorMod(recruit.tickCount, factor + 1) != 0;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private record Decision(int tick, boolean skip) {}

    private static synchronized boolean resolve() {
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
