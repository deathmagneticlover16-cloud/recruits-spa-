package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.resources.ResourceLocation;

/** Highest-priority, visual-only layer for targeted animation development. */
final class RecruitAnimationTestLayer extends ModifierLayer<IAnimation> {
    private final AbstractRecruitEntity recruit;
    private int activeSequence;
    private ClientAnimationTestState.Preview activePreview;

    RecruitAnimationTestLayer(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public Vec3f get3DTransform(String partName, TransformType type, float tickDelta, Vec3f value0) {
        if (activePreview == null) {
            return value0;
        }

        // Right-arm rotation is the one intentional pass-through so locomotion
        // can continue to control it. Every other animated rotation/position
        // starts from zero, preventing lower locomotion, action, and held-item
        // layers from being added to the Blockbench-authored preview pose.
        if (type == TransformType.ROTATION && "rightArm".equals(partName)) {
            return value0;
        }
        if (type == TransformType.ROTATION || type == TransformType.POSITION) {
            return super.get3DTransform(partName, type, tickDelta, new Vec3f(0.0F, 0.0F, 0.0F));
        }
        return super.get3DTransform(partName, type, tickDelta, value0);
    }

    @Override
    public void tick() {
        int entityId = recruit.getId();
        ClientAnimationTestState.Request request = ClientAnimationTestState.get(entityId);

        if (request != null && request.sequence != activeSequence) {
            if (startPreview(request.preview)) {
                activeSequence = request.sequence;
                activePreview = request.preview;
            }
        } else if (request == null && activePreview != null) {
            clearPreview();
        }

        super.tick();

        IAnimation animation = getAnimation();
        if (activePreview != null
                && !activePreview.holdsFinalFrame
                && (animation == null || !animation.isActive())) {
            ClientAnimationTestState.clear(entityId, activeSequence);
            clearPreview();
        }
    }

    private boolean startPreview(ClientAnimationTestState.Preview preview) {
        ResourceLocation id = new ResourceLocation(
                RecruitsSeriousAnimations.MOD_ID,
                preview.animationPath
        );
        KeyframeAnimation data = PlayerAnimationRegistry.getAnimation(id);
        if (data == null) {
            return false;
        }

        KeyframeAnimationPlayer player = new KeyframeAnimationPlayer(data);
        if (getAnimation() == null) {
            setAnimation(player);
        } else {
            replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(2, Ease.INOUTSINE),
                    player
            );
        }
        return true;
    }

    private void clearPreview() {
        setAnimation(null);
        activeSequence = 0;
        activePreview = null;
    }
}
