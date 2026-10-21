package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Matches Icy's player seat drop for Recruit-sized humanoids. */
final class IcysHorseRiderAnchor {
    @SubscribeEvent
    public void beforeRender(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof AbstractRecruitEntity recruit)
                || !EntityCompat.isIcysHorse(recruit.getVehicle())) {
            return;
        }
        double genericDrop = recruit.getMyRidingOffset() < 0.0D
                ? 0.25D - recruit.getMyRidingOffset()
                : recruit.getMyRidingOffset() == 0.0D && recruit.getBbHeight() >= 1.2F
                ? recruit.getBbHeight() / 3.0D
                : 0.0D;
        double correction = Math.max(0.0D, genericDrop - 0.6D);
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(0.0D, -correction, 0.0D);
    }

    @SubscribeEvent
    public void afterRender(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() instanceof AbstractRecruitEntity recruit
                && EntityCompat.isIcysHorse(recruit.getVehicle())) {
            event.getPoseStack().popPose();
        }
    }
}
