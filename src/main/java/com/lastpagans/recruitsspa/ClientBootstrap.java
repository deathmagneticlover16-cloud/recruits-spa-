package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import me.Thelnfamous1.mobplayeranimator.api.MobAnimationAccess;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.MinecraftForge;

public final class ClientBootstrap {
    private ClientBootstrap() {}

    public static void init() {
        MobAnimationAccess.REGISTER_ANIMATION_EVENT.register(ClientBootstrap::registerRecruitAnimations);
        MinecraftForge.EVENT_BUS.addListener(ClientAnimationTestCommands::register);
        MinecraftForge.EVENT_BUS.register(new IcysHorseRiderAnchor());
    }

    private static void registerRecruitAnimations(Mob mob, AnimationStack stack) {
        if (!(mob instanceof AbstractRecruitEntity recruit)) {
            return;
        }

        stack.addAnimLayer(0, new RecruitLocomotionLayer(recruit));
        stack.addAnimLayer(1000, new RecruitActionLayer(recruit));

        // Explicit/edge-triggered spear throw. It is completely inactive between throws
        // and cannot alter idle/movement.
        stack.addAnimLayer(3200, new RecruitSpearThrowLayer(recruit));
        stack.addAnimLayer(3300, new RecruitMountedTriumphLayer(recruit));
        stack.addAnimLayer(3500, new RecruitRoutingLayer(recruit));

        // Matched-combat must win over Better Mob Combat's attack layer (2000)
        // while the execution is active.
        stack.addAnimLayer(5000, new RecruitExecutionLayer(recruit));

        // Developer-only, client-local preview. It deliberately wins over every
        // normal layer so an animation can be inspected without AI interference.
        stack.addAnimLayer(10000, new RecruitAnimationTestLayer(recruit));
    }
}
