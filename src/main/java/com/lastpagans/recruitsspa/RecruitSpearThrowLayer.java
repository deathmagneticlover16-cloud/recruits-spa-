package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.MirrorModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Recruit spear/pilum throw reproducing SPA's real trident presentation.
 *
 * SPA itself does not contain a separate "trident throw" keyframe file. Its
 * player code combines:
 *   1) seriousplayeranimations:trident while charging,
 *   2) roughly 55 degrees of body yaw while charged,
 *   3) seriousplayeranimations:sword_attack when the trident is released.
 *
 * Earlier Recruits builds played only (1), which produced the infamous
 * "Olympic torch" pose followed by an unrelated custom hurl. This layer uses
 * the same three-part recipe, except the 55-degree turn is render-only so it
 * cannot fight Recruits/RTS target facing or formation AI.
 *
 * No entity position, navigation, velocity or yaw is changed here.
 */
final class RecruitSpearThrowLayer extends LodAwareModifierLayer {
    private static final ResourceLocation SPA_TRIDENT =
            new ResourceLocation("seriousplayeranimations", "trident");
    private static final ResourceLocation SPA_RELEASE =
            new ResourceLocation("seriousplayeranimations", "sword_attack");

    /** Fade/plant into SPA's charged trident pose, then hold it briefly. */
    private static final int WINDUP_TICKS = 12;
    private static final int WINDUP_FADE_TICKS = 5;
    /** SPA uses 1.4x for sword combat; 0.95x is intentionally more readable as a javelin throw. */
    private static final float RELEASE_SPEED = 0.95F;
    private static final float BODY_TWIST_RADIANS = (float) Math.toRadians(55.0D);
    private static final float TWIST_BLEND_PER_TICK = 0.24F;

    private final AbstractRecruitEntity recruit;
    private final MirrorModifier mirrorModifier;
    private final SpeedModifier speedModifier;

    private boolean mirrored;
    private int lastNetworkSequence;
    private boolean lastThrowableUsing;
    private boolean throwing;
    private boolean releasePhase;
    private int age;
    private float twistBlend;
    private float previousTwistBlend;

    RecruitSpearThrowLayer(AbstractRecruitEntity recruit) {
        super(recruit);
        this.recruit = recruit;
        this.mirrorModifier = new MirrorModifier(false);
        this.speedModifier = new SpeedModifier(1.0F);
        addModifierLast(mirrorModifier);
        addModifierLast(speedModifier);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public Vec3f get3DTransform(String partName, TransformType type, float tickDelta, Vec3f value0) {
        // A Betsy Ross standard must always keep Betsy Ross' own arm pose.
        if (BetsyRossCompat.isHoldingStandard(recruit) && BetsyRossCompat.isArmPart(partName)) {
            return value0;
        }

        Vec3f value = super.get3DTransform(partName, type, tickDelta, value0);
        if (!throwing) return value;

        float blend = previousTwistBlend + (twistBlend - previousTwistBlend) * tickDelta;
        if (blend <= 0.001F) return value;

        // Same side-on presentation SPA gets by changing the player's body yaw,
        // recreated as a model-only torso/shoulder turn for Recruits.
        float handSign = mirrored ? 1.0F : -1.0F;
        float twist = handSign * BODY_TWIST_RADIANS * smoothstep(blend);

        if (type == TransformType.ROTATION && "torso".equals(partName)) {
            return new Vec3f(
                    value.getX().floatValue(),
                    value.getY().floatValue() + twist,
                    value.getZ().floatValue()
            );
        }

        if (type == TransformType.POSITION && BetsyRossCompat.isArmPart(partName)) {
            float x = value.getX().floatValue();
            float y = value.getY().floatValue();
            float z = value.getZ().floatValue();
            float cos = (float) Math.cos(twist);
            float sin = (float) Math.sin(twist);
            return new Vec3f(x * cos + z * sin, y, -x * sin + z * cos);
        }

        return value;
    }

    @Override
    public void tick() {
        previousTwistBlend = twistBlend;

        int networkSequence = ClientSpearThrowState.sequence(recruit.getId());
        if (networkSequence != 0 && networkSequence != lastNetworkSequence) {
            lastNetworkSequence = networkSequence;
            start(recruit.getUsedItemHand() == InteractionHand.OFF_HAND);
        }

        ItemStack active = recruit.getUseItem();
        boolean throwableUsing = recruit.isUsingItem() && ThrowableWeaponDetector.isThrowable(active);
        if (throwableUsing && !lastThrowableUsing && !throwing) {
            start(recruit.getUsedItemHand() == InteractionHand.OFF_HAND);
        }
        lastThrowableUsing = throwableUsing;

        if (throwing) {
            age++;

            float twistTarget = releasePhase ? 0.0F : 1.0F;
            if (twistBlend < twistTarget) {
                twistBlend = Math.min(twistTarget, twistBlend + TWIST_BLEND_PER_TICK);
            } else if (twistBlend > twistTarget) {
                twistBlend = Math.max(twistTarget, twistBlend - TWIST_BLEND_PER_TICK);
            }

            if (!releasePhase && age >= WINDUP_TICKS) {
                beginRelease();
            }
            if (age >= RecruitsAnimationsApi.SPEAR_THROW_DURATION_TICKS) {
                stop();
            }
        } else {
            twistBlend = 0.0F;
        }

        super.tick();
    }

    private void start(boolean offhand) {
        // Never steal a Betsy Ross standard-bearing pose.
        if (BetsyRossCompat.isHoldingStandard(recruit)) return;

        KeyframeAnimation data = PlayerAnimationRegistry.getAnimation(SPA_TRIDENT);
        if (data == null) return;

        mirrored = offhand;
        mirrorModifier.setEnabled(offhand);
        speedModifier.speed = 1.0F;
        KeyframeAnimationPlayer player = new KeyframeAnimationPlayer(data);
        if (getAnimation() == null) {
            setAnimation(player);
        } else {
            replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(WINDUP_FADE_TICKS, Ease.INOUTSINE),
                    player
            );
        }

        throwing = true;
        releasePhase = false;
        age = 0;
        twistBlend = 0.0F;
        previousTwistBlend = 0.0F;
    }

    private void beginRelease() {
        KeyframeAnimation data = PlayerAnimationRegistry.getAnimation(SPA_RELEASE);
        if (data == null) {
            stop();
            return;
        }

        releasePhase = true;
        speedModifier.speed = RELEASE_SPEED;
        KeyframeAnimationPlayer player = new KeyframeAnimationPlayer(data);
        // SPA changes from charged trident to its attack swing immediately on
        // release. A one-tick fade hides arm discontinuity without muddying the hit.
        replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(1, Ease.INOUTSINE), player);
    }

    private void stop() {
        setAnimation(null);
        mirrored = false;
        mirrorModifier.setEnabled(false);
        speedModifier.speed = 1.0F;
        throwing = false;
        releasePhase = false;
        age = 0;
        twistBlend = 0.0F;
        previousTwistBlend = 0.0F;
    }

    private static float smoothstep(float value) {
        float t = Math.max(0.0F, Math.min(1.0F, value));
        return t * t * (3.0F - 2.0F * t);
    }
}
