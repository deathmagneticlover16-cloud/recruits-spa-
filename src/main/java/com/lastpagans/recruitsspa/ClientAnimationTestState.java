package com.lastpagans.recruitsspa;

import java.util.HashMap;
import java.util.Map;

/** Client-local animation requests used only by the preview commands. */
final class ClientAnimationTestState {
    enum Preview {
        R1_ENTER("shield_r1_enter", true),
        R1_LEAVE("shield_r1_leave", false),
        R2_ENTER("shield_r2_enter", true),
        R2_LEAVE("shield_r2_leave", false),
        R3_ENTER("shield_r3_enter", true),
        R3_LEAVE("shield_r3_leave", false);

        final String animationPath;
        final boolean holdsFinalFrame;

        Preview(String animationPath, boolean holdsFinalFrame) {
            this.animationPath = animationPath;
            this.holdsFinalFrame = holdsFinalFrame;
        }
    }

    static final class Request {
        final Preview preview;
        final int sequence;

        Request(Preview preview, int sequence) {
            this.preview = preview;
            this.sequence = sequence;
        }
    }

    private static final Map<Integer, Request> REQUESTS = new HashMap<>();
    private static int nextSequence = 1;

    private ClientAnimationTestState() {}

    static synchronized void play(int entityId, Preview preview) {
        REQUESTS.put(entityId, new Request(preview, nextSequence++));
    }

    static synchronized void clear(int entityId) {
        REQUESTS.remove(entityId);
    }

    static synchronized void clear(int entityId, int expectedSequence) {
        Request request = REQUESTS.get(entityId);
        if (request != null && request.sequence == expectedSequence) {
            REQUESTS.remove(entityId);
        }
    }

    static synchronized Request get(int entityId) {
        return REQUESTS.get(entityId);
    }

}
