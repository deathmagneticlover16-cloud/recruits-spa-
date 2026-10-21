package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.resources.ResourceLocation;

/** Visual-only panic overlay. Navigation and movement remain entirely Recruits/Commander owned. */
final class RecruitRoutingLayer extends LodAwareModifierLayer {
    private static final ResourceLocation ROUTING = new ResourceLocation(RecruitsSeriousAnimations.MOD_ID, "routing_overlay");
    private final AbstractRecruitEntity recruit;
    private boolean routing;

    RecruitRoutingLayer(AbstractRecruitEntity recruit) {
        super(recruit);
        this.recruit = recruit;
    }
    @Override public boolean isActive() { return true; }

    @Override
    public Vec3f get3DTransform(String partName, TransformType type, float tickDelta, Vec3f value0) {
        if (BetsyRossCompat.isHoldingStandard(recruit) && BetsyRossCompat.isArmPart(partName)) {
            return value0;
        }
        return super.get3DTransform(partName, type, tickDelta, value0);
    }

    @Override
    public void tick() {
        boolean shouldRoute = false;
        try { shouldRoute = recruit.getFleeing(); } catch (Throwable ignored) {}
        if (shouldRoute != routing) {
            if (shouldRoute) start(); else setAnimation(null);
            routing = shouldRoute;
        }
        super.tick();
    }

    private void start() {
        KeyframeAnimation data = PlayerAnimationRegistry.getAnimation(ROUTING);
        if (data == null) return;
        KeyframeAnimationPlayer player = new KeyframeAnimationPlayer(data);
        if (getAnimation() == null) setAnimation(player);
        else replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(2, Ease.INOUTSINE), player);
    }
}
