package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.resources.ResourceLocation;

/** Right-arm salute; item rotation changes while its normal hand anchor is preserved. */
final class RecruitMountedTriumphLayer extends LodAwareModifierLayer {
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(RecruitsSeriousAnimations.MOD_ID, "mounted_triumph_var_1");

    private final AbstractRecruitEntity recruit;
    private boolean playing;

    RecruitMountedTriumphLayer(AbstractRecruitEntity recruit) {
        super(recruit);
        this.recruit = recruit;
    }

    @Override
    public boolean isActive() {
        return playing || ClientMountedTriumphState.hasPending(recruit.getId());
    }

    @Override
    public Vec3f get3DTransform(String partName, TransformType type, float tickDelta, Vec3f value0) {
        if (BetsyRossCompat.isArmPart(partName) && BetsyRossCompat.isHoldingStandard(recruit)) {
            return value0;
        }
        return super.get3DTransform(partName, type, tickDelta, value0);
    }

    @Override
    public void tick() {
        int sequence = ClientMountedTriumphState.consume(recruit.getId());
        if (sequence != 0) {
            start();
        }

        if (playing && !EntityCompat.isHorseLike(recruit.getVehicle())) {
            stop();
        }

        super.tick();
        IAnimation animation = getAnimation();
        if (playing && (animation == null || !animation.isActive())) {
            stop();
        }
    }

    private void start() {
        if (!EntityCompat.isHorseLike(recruit.getVehicle()) || recruit.getMainHandItem().isEmpty()
                || BetsyRossCompat.isHoldingStandard(recruit)) {
            return;
        }
        KeyframeAnimation data = PlayerAnimationRegistry.getAnimation(ANIMATION);
        if (data == null) return;

        KeyframeAnimationPlayer player = new KeyframeAnimationPlayer(data);
        if (getAnimation() == null) {
            setAnimation(player);
        } else {
            replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(3, Ease.INOUTSINE), player);
        }
        playing = true;
    }

    private void stop() {
        setAnimation(null);
        playing = false;
    }
}
