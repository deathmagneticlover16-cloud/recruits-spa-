package com.lastpagans.recruitsspa;

import java.util.HashMap;
import java.util.Map;

/** Client-only edge triggers for mounted triumph animations. */
final class ClientMountedTriumphState {
    private static final Map<Integer, Integer> SEQUENCES = new HashMap<>();
    private static int nextSequence = 1;

    private ClientMountedTriumphState() {}

    static synchronized void trigger(int entityId) {
        SEQUENCES.put(entityId, nextSequence++);
    }

    static synchronized int sequence(int entityId) {
        return SEQUENCES.getOrDefault(entityId, 0);
    }
}
