package com.lastpagans.recruitsspa;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Client-only edge triggers for mounted triumph animations. */
final class ClientMountedTriumphState {
    private static final Map<Integer, Integer> SEQUENCES = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_SEQUENCE = new AtomicInteger(1);

    private ClientMountedTriumphState() {}

    static void trigger(int entityId) {
        SEQUENCES.put(entityId, NEXT_SEQUENCE.getAndIncrement());
    }

    static boolean hasPending(int entityId) {
        return SEQUENCES.containsKey(entityId);
    }

    static int consume(int entityId) {
        Integer sequence = SEQUENCES.remove(entityId);
        return sequence == null ? 0 : sequence;
    }

    static void clear(int entityId) {
        SEQUENCES.remove(entityId);
    }
}
