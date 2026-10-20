package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Optional Betsy Ross compatibility without a hard class dependency.
 *
 * Betsy Ross applies its own two-arm pose for FLAG_STANDARD/BANNER_STANDARD in
 * HumanoidModel.setupAnim. Mob Player Animator runs after that pose, so any
 * locomotion/action arm keys from this addon would overwrite it. When one of
 * Betsy Ross' StandardItem instances is in either hand, all normal Recruits SPA
 * layers pass the arm transforms through untouched.
 */
final class BetsyRossCompat {
    private static final String STANDARD_ITEM_CLASS =
            "rbasamoyai.betsyross.flags.standards.StandardItem";

    private BetsyRossCompat() {}

    static boolean isHoldingStandard(AbstractRecruitEntity recruit) {
        return recruit != null && (isStandard(recruit.getMainHandItem()) || isStandard(recruit.getOffhandItem()));
    }

    static boolean isStandard(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            Item item = stack.getItem();
            if (item == null) return false;
            Class<?> type = item.getClass();
            while (type != null) {
                if (STANDARD_ITEM_CLASS.equals(type.getName())) return true;
                type = type.getSuperclass();
            }
        } catch (Throwable ignored) {
            // Optional compatibility must never make animation rendering fatal.
        }
        return false;
    }

    static boolean isArmPart(String partName) {
        return "leftArm".equals(partName)
                || "rightArm".equals(partName)
                || "left_arm".equals(partName)
                || "right_arm".equals(partName);
    }
}
