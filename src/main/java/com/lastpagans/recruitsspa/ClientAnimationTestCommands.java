package com.lastpagans.recruitsspa;

import com.mojang.brigadier.CommandDispatcher;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

/** Client-only commands for previewing shield-wall rank animations. */
final class ClientAnimationTestCommands {
    private ClientAnimationTestCommands() {}

    static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("recruitspa")
                .then(Commands.literal("test")
                        .then(Commands.literal("r1").executes(context -> playLookedAt(
                                ClientAnimationTestState.Preview.R1_ENTER)))
                        .then(Commands.literal("r2").executes(context -> playLookedAt(
                                ClientAnimationTestState.Preview.R2_ENTER)))
                        .then(Commands.literal("r3").executes(context -> 0)))
                .then(Commands.literal("stop")
                        .then(Commands.literal("r1").executes(context -> playLookedAt(
                                ClientAnimationTestState.Preview.R1_LEAVE)))
                        .then(Commands.literal("r2").executes(context -> clearLookedAt()))
                        .then(Commands.literal("r3").executes(context -> 0))));
    }

    private static int playLookedAt(ClientAnimationTestState.Preview preview) {
        AbstractRecruitEntity recruit = lookedAtRecruit();
        if (recruit == null) {
            return 0;
        }
        ClientAnimationTestState.play(recruit.getId(), preview);
        return 1;
    }

    private static int clearLookedAt() {
        AbstractRecruitEntity recruit = lookedAtRecruit();
        if (recruit == null) {
            return 0;
        }
        ClientAnimationTestState.clear(recruit.getId());
        return 1;
    }

    private static AbstractRecruitEntity lookedAtRecruit() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return null;
        }

        HitResult hitResult = minecraft.hitResult;
        if (!(hitResult instanceof EntityHitResult entityHit)) {
            return null;
        }

        Entity entity = entityHit.getEntity();
        return entity instanceof AbstractRecruitEntity recruit ? recruit : null;
    }
}
