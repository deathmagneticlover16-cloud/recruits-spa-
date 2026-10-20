package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.resources.ResourceLocation;

/** Highest-priority, visual-only layer for targeted animation development. */
final class RecruitAnimationTestLayer extends ModifierLayer<IAnimation> {
    private static final ResourceLocation SHIELD_RANK_ONE =
            new ResourceLocation(RecruitsSeriousAnimations.MOD_ID, "shield_rank_one");

    private final AbstractRecruitEntity recruit;
    private boolean previewing;

    RecruitAnimationTestLayer(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void tick() {
        boolean shouldPreview = ClientAnimationTestState.isActive(recruit.m_19879_());
        if (shouldPreview && !previewing) {
            previewing = startPreview();
        } else if (!shouldPreview && previewing) {
            setAnimation(null);
            previewing = false;
        }
        super.tick();
    }

    private boolean startPreview() {
        KeyframeAnimation data = PlayerAnimationRegistry.getAnimation(SHIELD_RANK_ONE);
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
}
