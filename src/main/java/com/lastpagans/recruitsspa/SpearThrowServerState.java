package com.lastpagans.recruitsspa;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Server-side tick countdown used only to expose throw-active timing to Commander. */
public final class SpearThrowServerState {
    static final SpearThrowServerState INSTANCE = new SpearThrowServerState();
    private static final Map<Integer, Integer> REMAINING = new HashMap<>();

    private SpearThrowServerState() {}

    static synchronized void start(int entityId, int ticks) {
        REMAINING.put(entityId, Math.max(1, ticks));
    }

    public static synchronized boolean isActive(int entityId) {
        return REMAINING.getOrDefault(entityId, 0) > 0;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        synchronized (SpearThrowServerState.class) {
            Iterator<Map.Entry<Integer, Integer>> it = REMAINING.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Integer> e = it.next();
                int left = e.getValue() - 1;
                if (left <= 0) it.remove(); else e.setValue(left);
            }
        }
    }
}
