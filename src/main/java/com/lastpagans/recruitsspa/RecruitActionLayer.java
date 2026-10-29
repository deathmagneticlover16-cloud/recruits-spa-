package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.MirrorModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Upper-body/action layer for Recruits.
 *
 * Most SPA action files intentionally omit the legs, so the locomotion layer can
 * continue underneath sword swings, bow aiming, shield blocking and eating.
 */
final class RecruitActionLayer extends LodAwareModifierLayer {
    private static final Map<net.minecraft.world.item.Item, Boolean> LANCE_ITEMS = new IdentityHashMap<>();
    private static final float BOW_TORSO_TWIST = (float) Math.toRadians(55.0D);
    private static final float BOW_BLEND_PER_TICK = 0.22F;
    private static final ResourceLocation BLANK = spa("blank_loop");
    private static final ResourceLocation SWORD_1 = spa("sword_attack");
    private static final ResourceLocation SWORD_2 = spa("sword_attack2");
    private static final ResourceLocation SHIELD = spa("shield");
    private static final ResourceLocation BOW = spa("bow_idle");
    private static final ResourceLocation EAT = spa("eating_right");
    private static final ResourceLocation TRIDENT = spa("trident");

    // SPA has no dedicated crossbow animation, so v0.3 adds two tiny native
    // Player Animator poses under our own namespace.
    private static final ResourceLocation CROSSBOW_HOLD = ours("crossbow_hold");
    private static final ResourceLocation CROSSBOW_CHARGE = ours("crossbow_charge");
    private static final ResourceLocation MOUNTED_MELEE_READY = ours("mounted_melee_ready");

    private final AbstractRecruitEntity recruit;
    private final MirrorModifier mirrorModifier;
    private final SpeedModifier speedModifier;

    private Action action;
    private boolean lastSwinging;
    private boolean alternateSword;
    private boolean oneShot;
    private float bowBlend;
    private float previousBowBlend;

    RecruitActionLayer(AbstractRecruitEntity recruit) {
        super(recruit);
        this.recruit = recruit;
        this.mirrorModifier = new MirrorModifier(false);
        this.speedModifier = new SpeedModifier(1.0F);
        addModifierLast(this.mirrorModifier);
        addModifierLast(this.speedModifier);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public Vec3f get3DTransform(String partName, TransformType type, float tickDelta, Vec3f value0) {
        // Emergency Betsy Ross compatibility: its HumanoidModel mixin already
        // positions both arms for flag/banner standards. Do not overwrite that
        // native pose from any of this layer's animations.
        if (BetsyRossCompat.isArmPart(partName) && BetsyRossCompat.isHoldingStandard(recruit)) {
            return value0;
        }

        Vec3f value = super.get3DTransform(partName, type, tickDelta, value0);

        float blend = previousBowBlend + (bowBlend - previousBowBlend) * tickDelta;
        if (blend <= 0.001F) {
            return value;
        }

        // SPA gets its characteristic side-on archery posture by rotating the
        // player's body yaw in Java. Doing that to a Recruit entity fights its AI
        // target-facing code, so v0.3.4 reproduces only the *rendered* torso twist.
        // The arm rotations remain Recruits' vanilla target-aware BOW_AND_ARROW
        // rotations; only the shoulder pivots follow the turned chest.
        float handSign = recruit.getUsedItemHand() == InteractionHand.OFF_HAND ? 1.0F : -1.0F;
        float twist = handSign * BOW_TORSO_TWIST * smoothstep(blend);

        if (type == TransformType.ROTATION && "torso".equals(partName)) {
            return new Vec3f(
                    value.getX().floatValue(),
                    value.getY().floatValue() + twist,
                    value.getZ().floatValue()
            );
        }

        if (type == TransformType.POSITION && isArmPart(partName)) {
            float x = value.getX().floatValue();
            float y = value.getY().floatValue();
            float z = value.getZ().floatValue();
            float cos = (float) Math.cos(twist);
            float sin = (float) Math.sin(twist);

            // Rotate the shoulder pivot around the torso centre so the arms stay
            // attached to the visually turned chest without changing their aim.
            return new Vec3f(x * cos + z * sin, y, -x * sin + z * cos);
        }

        return value;
    }

    private static boolean isArmPart(String partName) {
        return "leftArm".equals(partName)
                || "rightArm".equals(partName)
                || "left_arm".equals(partName)
                || "right_arm".equals(partName);
    }

    private static float smoothstep(float value) {
        float t = Math.max(0.0F, Math.min(1.0F, value));
        return t * t * (3.0F - 2.0F * t);
    }

    private boolean isActivelyDrawingBow() {
        if (!recruit.isUsingItem()) {
            return false;
        }
        ItemStack active = recruit.getUseItem();
        return "BOW".equals(useAnimationName(active));
    }

    @Override
    public void tick() {
        previousBowBlend = bowBlend;

        // Betsy Ross standards own their arm animation completely. Clear any
        // lingering action animation so a prior sword/use pose cannot leak into
        // the standard-bearing pose after an item swap.
        if (BetsyRossCompat.isHoldingStandard(recruit)) {
            bowBlend = 0.0F;
            oneShot = false;
            mirrorModifier.setEnabled(false);
            ensureLoop(Action.NONE);
            lastSwinging = recruit.swinging;
            super.tick();
            return;
        }
        float bowTarget = isActivelyDrawingBow() ? 1.0F : 0.0F;
        if (bowBlend < bowTarget) {
            bowBlend = Math.min(bowTarget, bowBlend + BOW_BLEND_PER_TICK);
        } else if (bowBlend > bowTarget) {
            bowBlend = Math.max(bowTarget, bowBlend - BOW_BLEND_PER_TICK);
        }

        boolean swinging = recruit.swinging;
        boolean swingStarted = swinging && !lastSwinging;
        lastSwinging = swinging;

        Action useAction = detectUseAction();

        if (useAction != Action.NONE) {
            oneShot = false;
            ensureLoop(useAction);
        } else if (swingStarted && shouldPlayMeleeFallback()) {
            alternateSword = !alternateSword;
            startOneShot(alternateSword ? Action.SWORD_1 : Action.SWORD_2);
        } else if (oneShot) {
            IAnimation current = getAnimation();
            if (current == null || !current.isActive()) {
                oneShot = false;
                ensureLoop(Action.NONE);
            }
        } else {
            Action passive = detectPassiveAction();
            ensureLoop(passive);
        }

        super.tick();
    }

    private Action detectUseAction() {
        if (!recruit.isUsingItem()) {
            return Action.NONE;
        }

        ItemStack active = recruit.getUseItem();
        String use = useAnimationName(active);
        boolean left = recruit.getUsedItemHand() == InteractionHand.OFF_HAND;
        mirrorModifier.setEnabled(left);

        // Recruits keeps control of the target-aware bow arm rotations. A render-only
        // torso/shoulder twist is added in get3DTransform() to recover SPA's side-on
        // silhouette without mutating the recruit entity's yaw.
        if ("BOW".equals(use)) {
            mirrorModifier.setEnabled(false);
            return Action.NONE;
        }

        return switch (use) {
            case "BLOCK" -> Action.SHIELD;
            case "CROSSBOW" -> Action.CROSSBOW_CHARGE;
            case "SPEAR" -> Action.TRIDENT;
            case "EAT", "DRINK" -> Action.EAT;
            default -> Action.NONE;
        };
    }

    private Action detectPassiveAction() {
        mirrorModifier.setEnabled(false);

        // Crossbowmen keep a ready pose only while they actually have a target.
        // This avoids making them aim their crossbow while casually travelling.
        ItemStack main = recruit.getMainHandItem();
        if (recruit.getTarget() != null && "CROSSBOW".equals(useAnimationName(main))) {
            return Action.CROSSBOW_HOLD;
        }

        // This item offset is authored specifically for couched lances. Applying
        // it to every mounted melee weapon moves swords, axes and pikes away from
        // their normal hand transforms.
        if (recruit.getTarget() != null
                && recruit.isPassenger()
                && isHorseLike(recruit.getVehicle())
                && isLance(main)) {
            return Action.MOUNTED_MELEE_READY;
        }
        return Action.NONE;
    }

    private static boolean isLance(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return LANCE_ITEMS.computeIfAbsent(stack.getItem(), item -> {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            return itemId != null && itemId.getPath().contains("lance");
        });
    }

    private boolean shouldPlayMeleeFallback() {
        if (BetsyRossCompat.isHoldingStandard(recruit)) {
            return false;
        }
        ItemStack main = recruit.getMainHandItem();
        if (main == null || main.isEmpty()) {
            return false;
        }

        String use = useAnimationName(main);
        // Ranged/use-action weapons get their own poses. Everything else that
        // actually triggers a vanilla swing gets SPA's sword attack as a safe
        // fallback. Better Mob Combat's priority-2000 attack still overrides it.
        return !"BOW".equals(use)
                && !"CROSSBOW".equals(use)
                && !"BLOCK".equals(use)
                && !"EAT".equals(use)
                && !"DRINK".equals(use);
    }

    private void ensureLoop(Action target) {
        if (action == target && getAnimation() != null && getAnimation().isActive()) {
            speedModifier.speed = target.speed;
            return;
        }

        KeyframeAnimation data = PlayerAnimationRegistry.getAnimation(target.animation);
        if (data == null) {
            return;
        }

        speedModifier.speed = target.speed;
        KeyframeAnimationPlayer player = new KeyframeAnimationPlayer(data);
        if (getAnimation() == null) {
            setAnimation(player);
        } else {
            replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(target.fadeTicks, Ease.INOUTSINE),
                    player
            );
        }
        action = target;
    }

    private void startOneShot(Action target) {
        KeyframeAnimation data = PlayerAnimationRegistry.getAnimation(target.animation);
        if (data == null) {
            return;
        }

        mirrorModifier.setEnabled(false);
        speedModifier.speed = target.speed;
        KeyframeAnimationPlayer player = new KeyframeAnimationPlayer(data);
        if (getAnimation() == null) {
            setAnimation(player);
        } else {
            replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(target.fadeTicks, Ease.INOUTSINE),
                    player
            );
        }
        action = target;
        oneShot = true;
    }

    private static String useAnimationName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "NONE";
        }
        UseAnim anim = stack.getUseAnimation();
        return anim == null ? "NONE" : anim.name();
    }

    private static boolean isHorseLike(net.minecraft.world.entity.Entity vehicle) {
        return EntityCompat.isHorseLike(vehicle);
    }

    private static ResourceLocation spa(String path) {
        return new ResourceLocation("seriousplayeranimations", path);
    }

    private static ResourceLocation ours(String path) {
        return new ResourceLocation(RecruitsSeriousAnimations.MOD_ID, path);
    }

    private enum Action {
        NONE(BLANK, 2, 1.0F),
        SWORD_1(RecruitActionLayer.SWORD_1, 0, 1.4F),
        SWORD_2(RecruitActionLayer.SWORD_2, 0, 1.4F),
        SHIELD(RecruitActionLayer.SHIELD, 4, 1.0F),
        EAT(RecruitActionLayer.EAT, 4, 1.0F),
        TRIDENT(RecruitActionLayer.TRIDENT, 3, 1.0F),
        CROSSBOW_HOLD(RecruitActionLayer.CROSSBOW_HOLD, 3, 1.0F),
        CROSSBOW_CHARGE(RecruitActionLayer.CROSSBOW_CHARGE, 2, 1.0F),
        MOUNTED_MELEE_READY(RecruitActionLayer.MOUNTED_MELEE_READY, 3, 1.0F);

        private final ResourceLocation animation;
        private final int fadeTicks;
        private final float speed;

        Action(ResourceLocation animation, int fadeTicks, float speed) {
            this.animation = animation;
            this.fadeTicks = fadeTicks;
            this.speed = speed;
        }
    }
}
