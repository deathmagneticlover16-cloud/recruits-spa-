package com.lastpagans.recruitsspa;

import java.util.HashSet;
import java.util.Set;

/** Client-local selection used only by the animation preview command. */
final class ClientAnimationTestState {
    private static final Set<Integer> ACTIVE_RECRUITS = new HashSet<>();

    private ClientAnimationTestState() {}

    static synchronized void start(int entityId) {
        ACTIVE_RECRUITS.add(entityId);
    }

    static synchronized void stop(int entityId) {
        ACTIVE_RECRUITS.remove(entityId);
    }

    static synchronized boolean isActive(int entityId) {
        return ACTIVE_RECRUITS.contains(entityId);
    }
}
