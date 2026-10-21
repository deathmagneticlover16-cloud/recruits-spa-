package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Matches Icy's player seat drop for Recruit-sized humanoids. */
final class IcysHorseRiderAnchor {
    /*
     * Icy already subtracts its 0.6-block humanoid seat drop from the entity
     * position. Mob Player Animator's Recruit model origin still renders above
     * that seat, so it needs this additional model-space correction.
     */
    private static final double RECRUIT_MODEL_SEAT_DROP = 0.45D;

    @SubscribeEvent
    public void beforeRender(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof AbstractRecruitEntity recruit)
                || !EntityCompat.isIcysHorse(recruit.getVehicle())) {
            return;
        }
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(0.0D, -RECRUIT_MODEL_SEAT_DROP, 0.0D);
    }

    @SubscribeEvent
    public void afterRender(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() instanceof AbstractRecruitEntity recruit
                && EntityCompat.isIcysHorse(recruit.getVehicle())) {
            event.getPoseStack().popPose();
        }
    }
}
