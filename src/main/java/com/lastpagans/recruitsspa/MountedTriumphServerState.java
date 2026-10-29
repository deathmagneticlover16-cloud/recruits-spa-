package com.lastpagans.recruitsspa;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side activity window exposed to RTS integrations. */
public final class MountedTriumphServerState {
    static final MountedTriumphServerState INSTANCE = new MountedTriumphServerState();
    private static final Map<Integer, Integer> REMAINING = new ConcurrentHashMap<>();

    private MountedTriumphServerState() {}

    static void start(int entityId, int ticks) {
        REMAINING.put(entityId, Math.max(1, ticks));
    }

    public static boolean isActive(int entityId) {
        return REMAINING.getOrDefault(entityId, 0) > 0;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || REMAINING.isEmpty()) return;
        for (Integer entityId : REMAINING.keySet()) {
            REMAINING.computeIfPresent(entityId,
                    (ignored, left) -> left <= 1 ? null : left - 1);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        REMAINING.clear();
    }
}
