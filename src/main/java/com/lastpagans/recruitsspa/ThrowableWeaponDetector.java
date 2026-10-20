package com.lastpagans.recruitsspa;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Fail-soft detector for Commander throwable weapons.
 * Prefers #recruits_rts_camera:throwable_weapons and falls back to the known
 * spear/javelin naming used by the current test items.
 */
final class ThrowableWeaponDetector {
    private static final String TAG_NAMESPACE = "recruits_rts_camera";
    private static final String TAG_PATH = "throwable_weapons";
    private static final Map<String, Object> TAGS = new HashMap<>();
    private static volatile boolean itemStackMethodResolved;
    private static Method itemStackIsTag;

    private ThrowableWeaponDetector() {}

    static boolean isThrowable(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return false;
        }
        if (isTagged(stack, TAG_NAMESPACE, TAG_PATH)) {
            return true;
        }
        Object item = stack.m_41720_();
        if (item == null) {
            return false;
        }
        String name = (item.getClass().getSimpleName() + " " + item).toLowerCase();
        return name.contains("antique_spear")
                || name.contains("triarii_spear")
                || name.contains("javelin")
                || name.contains("pilum");
    }

    private static boolean isTagged(ItemStack stack, String namespace, String path) {
        try {
            Object tag = tag(namespace, path);
            Method method = itemStackTagMethod(tag);
            if (tag == null || method == null) {
                return false;
            }
            Object result = method.invoke(stack, tag);
            return result instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static synchronized Object tag(String namespace, String path) {
        String key = namespace + ':' + path;
        if (TAGS.containsKey(key)) {
            return TAGS.get(key);
        }
        Object value = null;
        try {
            Class<?> itemTags = Class.forName("net.minecraft.tags.ItemTags", false,
                    ThrowableWeaponDetector.class.getClassLoader());
            ResourceLocation id = new ResourceLocation(namespace, path);
            for (Method method : itemTags.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) {
                    continue;
                }
                if (method.getParameterTypes()[0] != ResourceLocation.class) {
                    continue;
                }
                if (!"net.minecraft.tags.TagKey".equals(method.getReturnType().getName())) {
                    continue;
                }
                method.setAccessible(true);
                value = method.invoke(null, id);
                break;
            }
        } catch (Throwable ignored) {
            value = null;
        }
        TAGS.put(key, value);
        return value;
    }

    private static Method itemStackTagMethod(Object tag) {
        if (tag == null) {
            return null;
        }
        if (!itemStackMethodResolved) {
            synchronized (ThrowableWeaponDetector.class) {
                if (!itemStackMethodResolved) {
                    itemStackMethodResolved = true;
                    for (Method method : ItemStack.class.getMethods()) {
                        if (method.getReturnType() != boolean.class || method.getParameterCount() != 1) {
                            continue;
                        }
                        if ("net.minecraft.tags.TagKey".equals(method.getParameterTypes()[0].getName())) {
                            method.setAccessible(true);
                            itemStackIsTag = method;
                            break;
                        }
                    }
                }
            }
        }
        return itemStackIsTag;
    }
}
