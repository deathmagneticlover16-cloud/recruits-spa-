package com.lastpagans.recruitsspa;

import java.util.HashMap;
import java.util.Map;

/** Tiny client-only edge-trigger table for spear throws. */
final class ClientSpearThrowState {
    private static final Map<Integer, Integer> SEQUENCES = new HashMap<>();
    private static int nextSequence = 1;

    private ClientSpearThrowState() {}

    static synchronized void trigger(int entityId) {
        SEQUENCES.put(entityId, nextSequence++);
    }

    static synchronized int sequence(int entityId) {
        return SEQUENCES.getOrDefault(entityId, 0);
    }
}
