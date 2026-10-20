package com.lastpagans.recruitsspa;

import de.maxhenkel.recruits.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

public final class ExecutionSyncMessage implements Message<ExecutionSyncMessage> {
    static final int END = 0;
    static final int START = 1;
    static final int RELEASE_ATTACKER = 2;

    static final int SFX_STAB = 10;
    static final int SFX_CLASH = 11;
    static final int SFX_PAIN = 12;
    static final int SFX_DEATH = 13;
    static final int SFX_BODY_FALL = 14;
    static final int SFX_SHIELD = 15;

    private int attackerId;
    private int victimId;
    private int phase;
    private int executionType;

    public ExecutionSyncMessage() {}

    ExecutionSyncMessage(int attackerId, int victimId, int phase, int executionType) {
        this.attackerId = attackerId;
        this.victimId = victimId;
        this.phase = phase;
        this.executionType = executionType;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        ClientExecutionState.handle(attackerId, victimId, phase, executionType);
    }

    @Override
    public ExecutionSyncMessage fromBytes(FriendlyByteBuf buf) {
        attackerId = buf.readInt();
        victimId = buf.readInt();
        phase = buf.readInt();
        executionType = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(attackerId);
        buf.writeInt(victimId);
        buf.writeInt(phase);
        buf.writeInt(executionType);
    }
}
