package com.lastpagans.recruitsspa;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Server-side activity window exposed to RTS integrations. */
public final class MountedTriumphServerState {
    static final MountedTriumphServerState INSTANCE = new MountedTriumphServerState();
    private static final Map<Integer, Integer> REMAINING = new HashMap<>();

    private MountedTriumphServerState() {}

    static synchronized void start(int entityId, int ticks) {
        REMAINING.put(entityId, Math.max(1, ticks));
    }

    public static synchronized boolean isActive(int entityId) {
        return REMAINING.getOrDefault(entityId, 0) > 0;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        synchronized (MountedTriumphServerState.class) {
            Iterator<Map.Entry<Integer, Integer>> iterator = REMAINING.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Integer> entry = iterator.next();
                int remaining = entry.getValue() - 1;
                if (remaining <= 0) iterator.remove(); else entry.setValue(remaining);
            }
        }
    }
}
