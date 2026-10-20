package com.lastpagans.recruitsspa;

import java.util.HashMap;
import java.util.Map;

/** Client-side execution role/type table populated by ExecutionSyncMessage. */
final class ClientExecutionState {
    enum Role { ATTACKER, VICTIM }

    static final class State {
        final Role role;
        final int sequence;
        final int executionType;

        State(Role role, int sequence, int executionType) {
            this.role = role;
            this.sequence = sequence;
            this.executionType = executionType;
        }
    }

    private static final Map<Integer, State> STATES = new HashMap<>();
    private static int nextSequence = 1;

    private ClientExecutionState() {}

    static synchronized void handle(int attackerId, int victimId, int phase, int executionType) {
        if (phase == ExecutionSyncMessage.START) {
            int sequence = nextSequence++;
            STATES.put(attackerId, new State(Role.ATTACKER, sequence, executionType));
            STATES.put(victimId, new State(Role.VICTIM, sequence, executionType));
            return;
        }

        if (phase == ExecutionSyncMessage.RELEASE_ATTACKER) {
            STATES.remove(attackerId);
            return;
        }

        if (phase == ExecutionSyncMessage.END) {
            STATES.remove(attackerId);
            STATES.remove(victimId);
            return;
        }

        // Semantic sound cues. These are intentionally independent from concrete
        // filenames so resource packs can replace the samples without changing code.
        switch (phase) {
            case ExecutionSyncMessage.SFX_STAB ->
                    ClientExecutionAudio.play(victimId, CombatSounds.STAB_FLESH, 1.0F, 0.055F);
            case ExecutionSyncMessage.SFX_CLASH ->
                    ClientExecutionAudio.play(attackerId, CombatSounds.WEAPON_CLASH, 0.85F, 0.045F);
            case ExecutionSyncMessage.SFX_PAIN ->
                    ClientExecutionAudio.play(victimId, CombatSounds.PAIN_VOCAL, 0.92F, 0.04F);
            case ExecutionSyncMessage.SFX_DEATH ->
                    ClientExecutionAudio.play(victimId, CombatSounds.DEATH_VOCAL, 1.0F, 0.035F);
            case ExecutionSyncMessage.SFX_BODY_FALL ->
                    ClientExecutionAudio.play(victimId, CombatSounds.BODY_FALL, 0.9F, 0.05F);
            case ExecutionSyncMessage.SFX_SHIELD ->
                    ClientExecutionAudio.play(victimId, CombatSounds.SHIELD_HIT, 0.9F, 0.06F);
            default -> {
            }
        }
    }

    static synchronized State get(int entityId) {
        return STATES.get(entityId);
    }
}
