package com.lastpagans.recruitsspa;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Optional client-side bridge into Better Combat's loaded attack animation map.
 * Reflection keeps Better Combat optional; the execution layer has local fallbacks.
 */
final class BetterCombatAnimationBridge {
    private static volatile boolean lookupAttempted;
    private static volatile Field animationsField;

    private BetterCombatAnimationBridge() {}

    static KeyframeAnimation get(String id) {
        try {
            Field field = animationField();
            if (field == null) {
                return null;
            }
            Object value = field.get(null);
            if (!(value instanceof Map<?, ?> map)) {
                return null;
            }
            Object animation = map.get(id);
            return animation instanceof KeyframeAnimation keyframe ? keyframe : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field animationField() {
        if (!lookupAttempted) {
            synchronized (BetterCombatAnimationBridge.class) {
                if (!lookupAttempted) {
                    lookupAttempted = true;
                    try {
                        Class<?> registry = Class.forName("net.bettercombat.client.animation.AnimationRegistry");
                        Field field = registry.getField("animations");
                        field.setAccessible(true);
                        animationsField = field;
                    } catch (Throwable ignored) {
                        animationsField = null;
                    }
                }
            }
        }
        return animationsField;
    }
}
