package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Drops static client animation state as soon as a Recruit leaves the client level. */
final class ClientAnimationStateCleanup {
    @SubscribeEvent
    public void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractRecruitEntity recruit)) {
            return;
        }
        int entityId = recruit.getId();
        ClientSpearThrowState.clear(entityId);
        ClientMountedTriumphState.clear(entityId);
        ClientExecutionState.clear(entityId);
        ClientAnimationTestState.clear(entityId);
        EmfAnimationLodCompat.clear(recruit);
        BetsyRossCompat.clear(recruit);
    }
}
