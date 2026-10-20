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

/** Small client-only command surface for previewing the current test animation. */
final class ClientAnimationTestCommands {
    private ClientAnimationTestCommands() {}

    static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("recruitspa")
                .then(Commands.literal("test").executes(context -> setLookedAtRecruit(true)))
                .then(Commands.literal("stop").executes(context -> setLookedAtRecruit(false))));
    }

    private static int setLookedAtRecruit(boolean active) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft == null) {
            return 0;
        }

        HitResult hitResult = minecraft.f_91077_;
        if (!(hitResult instanceof EntityHitResult entityHit)) {
            return 0;
        }

        Entity entity = entityHit.m_82443_();
        if (!(entity instanceof AbstractRecruitEntity)) {
            return 0;
        }

        int entityId = entity.m_19879_();
        if (active) {
            ClientAnimationTestState.start(entityId);
        } else {
            ClientAnimationTestState.stop(entityId);
        }
        return 1;
    }
}
