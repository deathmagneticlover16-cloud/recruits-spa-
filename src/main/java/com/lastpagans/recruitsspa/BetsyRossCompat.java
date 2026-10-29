package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

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
    private static final ClassValue<Boolean> STANDARD_CLASSES = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            Class<?> current = type;
            while (current != null) {
                if (STANDARD_ITEM_CLASS.equals(current.getName())) {
                    return true;
                }
                current = current.getSuperclass();
            }
            return false;
        }
    };
    private static final Map<AbstractRecruitEntity, HoldingDecision> HOLDING_CACHE = new WeakHashMap<>();

    private BetsyRossCompat() {}

    static boolean isHoldingStandard(AbstractRecruitEntity recruit) {
        if (recruit == null) return false;
        Item mainItem = item(recruit.getMainHandItem());
        Item offhandItem = item(recruit.getOffhandItem());
        HoldingDecision cached = HOLDING_CACHE.get(recruit);
        if (cached != null && cached.tick == recruit.tickCount
                && cached.mainItem == mainItem && cached.offhandItem == offhandItem) {
            return cached.holding;
        }
        boolean holding = isStandard(mainItem) || isStandard(offhandItem);
        if (cached == null) {
            HOLDING_CACHE.put(recruit, new HoldingDecision(recruit.tickCount, mainItem, offhandItem, holding));
        } else {
            cached.tick = recruit.tickCount;
            cached.mainItem = mainItem;
            cached.offhandItem = offhandItem;
            cached.holding = holding;
        }
        return holding;
    }

    static boolean isStandard(ItemStack stack) {
        return isStandard(item(stack));
    }

    static void clear(AbstractRecruitEntity recruit) {
        HOLDING_CACHE.remove(recruit);
    }

    private static Item item(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : stack.getItem();
    }

    private static boolean isStandard(Item item) {
        return item != null && STANDARD_CLASSES.get(item.getClass());
    }

    static boolean isArmPart(String partName) {
        return "leftArm".equals(partName)
                || "rightArm".equals(partName)
                || "left_arm".equals(partName)
                || "right_arm".equals(partName);
    }

    private static final class HoldingDecision {
        private int tick;
        private Item mainItem;
        private Item offhandItem;
        private boolean holding;

        private HoldingDecision(int tick, Item mainItem, Item offhandItem, boolean holding) {
            this.tick = tick;
            this.mainItem = mainItem;
            this.offhandItem = offhandItem;
            this.holding = holding;
        }
    }
}
