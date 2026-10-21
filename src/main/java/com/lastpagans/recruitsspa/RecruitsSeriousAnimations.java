package com.lastpagans.recruitsspa;

import de.maxhenkel.recruits.corelib.CommonRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(RecruitsSeriousAnimations.MOD_ID)
public final class RecruitsSeriousAnimations {
    public static final String MOD_ID = "recruits_serious_animations";
    public static SimpleChannel EXECUTION_CHANNEL;

    public RecruitsSeriousAnimations() {
        CombatSounds.register();

        // Common/server-safe channel. Recruits bundles the small CoreLib helper we
        // use here, avoiding a second networking dependency.
        EXECUTION_CHANNEL = CommonRegistry.registerChannel(MOD_ID, "execution", 8);
        CommonRegistry.registerMessage(EXECUTION_CHANNEL, 0, ExecutionSyncMessage.class);
        CommonRegistry.registerMessage(EXECUTION_CHANNEL, 1, SpearThrowSyncMessage.class);

        MinecraftForge.EVENT_BUS.register(ExecutionManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(SpearThrowServerState.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new IcysBetterHorsesCompat());

        // Keep all Player Animator / Mob Player Animator references out of the
        // dedicated-server class path. ClientBootstrap is loaded only on a client.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> bootstrap = Class.forName("com.lastpagans.recruitsspa.ClientBootstrap");
                bootstrap.getMethod("init").invoke(null);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to initialize Recruits: Serious Animations client bridge", e);
            }
        }
    }
}
