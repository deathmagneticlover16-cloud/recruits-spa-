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
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return 0;
        }

        HitResult hitResult = minecraft.hitResult;
        if (!(hitResult instanceof EntityHitResult entityHit)) {
            return 0;
        }

        Entity entity = entityHit.getEntity();
        if (!(entity instanceof AbstractRecruitEntity)) {
            return 0;
        }

        int entityId = entity.getId();
        if (active) {
            ClientAnimationTestState.start(entityId);
        } else {
            ClientAnimationTestState.stop(entityId);
        }
        return 1;
    }
}
