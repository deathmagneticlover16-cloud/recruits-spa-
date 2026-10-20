package com.lastpagans.recruitsspa;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraftforge.network.PacketDistributor;

/**
 * Small server-safe semantic API used by Commander/RTS.
 * Animation implementation details remain private to this addon.
 */
public final class RecruitsAnimationsApi {
    /** SPA-trident wind-up followed by the visual hand release. */
    public static final int SPEAR_THROW_RELEASE_TICK = 19;
    public static final int SPEAR_THROW_DURATION_TICKS = 31;

    private RecruitsAnimationsApi() {}

    /** Start the recruit's one-shot spear/pilum throw animation on clients. */
    public static void playSpearThrow(AbstractRecruitEntity recruit) {
        if (recruit == null) return;
        SpearThrowServerState.start(recruit.getId(), SPEAR_THROW_DURATION_TICKS);
        RecruitsSeriousAnimations.EXECUTION_CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                new SpearThrowSyncMessage(recruit.getId())
        );
    }

    /** Tick at which Commander should release/spawn the projectile after playSpearThrow(). */
    public static int spearThrowReleaseTick() { return SPEAR_THROW_RELEASE_TICK; }

    /** Total visual lock/hold window for Commander. */
    public static int spearThrowDurationTicks() { return SPEAR_THROW_DURATION_TICKS; }

    /** Server-side activity query. Intended for Commander movement-hold logic. */
    public static boolean isSpearThrowActive(AbstractRecruitEntity recruit) {
        return recruit != null && SpearThrowServerState.isActive(recruit.getId());
    }
}
