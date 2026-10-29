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
    /** TriumphVar1 is authored at 2.25 seconds. */
    public static final int MOUNTED_TRIUMPH_DURATION_TICKS = 45;

    private RecruitsAnimationsApi() {}

    /** Start the recruit's one-shot spear/pilum throw animation on clients. */
    public static void playSpearThrow(AbstractRecruitEntity recruit) {
        if (recruit == null) return;
        SpearThrowServerState.start(recruit.getId(), SPEAR_THROW_DURATION_TICKS);
        RecruitsSeriousAnimations.EXECUTION_CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY.with(() -> recruit),
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

    /**
     * Plays the right-hand mounted victory wave. Returns false when its visual
     * requirements are not met, allowing the caller to choose another action.
     */
    public static boolean playMountedTriumph(AbstractRecruitEntity recruit) {
        if (!canPlayMountedTriumph(recruit)) return false;
        MountedTriumphServerState.start(recruit.getId(), MOUNTED_TRIUMPH_DURATION_TICKS);
        RecruitsSeriousAnimations.EXECUTION_CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY.with(() -> recruit),
                new MountedTriumphSyncMessage(recruit.getId())
        );
        return true;
    }

    /** True when the Recruit is on a horse and has an item in its right hand. */
    public static boolean canPlayMountedTriumph(AbstractRecruitEntity recruit) {
        return recruit != null
                && EntityCompat.isHorseLike(recruit.getVehicle())
                && !recruit.getMainHandItem().isEmpty();
    }

    public static int mountedTriumphDurationTicks() { return MOUNTED_TRIUMPH_DURATION_TICKS; }

    public static boolean isMountedTriumphActive(AbstractRecruitEntity recruit) {
        return recruit != null && MountedTriumphServerState.isActive(recruit.getId());
    }
}
