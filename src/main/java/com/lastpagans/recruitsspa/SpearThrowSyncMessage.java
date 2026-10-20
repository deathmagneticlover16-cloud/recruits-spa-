package com.lastpagans.recruitsspa;

import de.maxhenkel.recruits.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

/** Server -> client edge trigger for the one-shot spear/pilum throw animation. */
public final class SpearThrowSyncMessage implements Message<SpearThrowSyncMessage> {
    private int entityId;

    public SpearThrowSyncMessage() {}

    SpearThrowSyncMessage(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        ClientSpearThrowState.trigger(entityId);
    }

    @Override
    public SpearThrowSyncMessage fromBytes(FriendlyByteBuf buf) {
        entityId = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }
}
