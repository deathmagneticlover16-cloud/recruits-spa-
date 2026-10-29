package com.lastpagans.recruitsspa;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static final Map<Integer, Request> REQUESTS = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_SEQUENCE = new AtomicInteger(1);

    private ClientAnimationTestState() {}

    static void play(int entityId, Preview preview) {
        REQUESTS.put(entityId, new Request(preview, NEXT_SEQUENCE.getAndIncrement()));
    }

    static void clear(int entityId) {
        REQUESTS.remove(entityId);
    }

    static void clear(int entityId, int expectedSequence) {
        REQUESTS.computeIfPresent(entityId,
                (ignored, request) -> request.sequence == expectedSequence ? null : request);
    }

    static Request get(int entityId) {
        return REQUESTS.get(entityId);
    }

}
