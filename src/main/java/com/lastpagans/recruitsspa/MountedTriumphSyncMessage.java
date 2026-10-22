package com.lastpagans.recruitsspa;

import de.maxhenkel.recruits.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

/** Server-to-client edge trigger for the mounted victory wave. */
public final class MountedTriumphSyncMessage implements Message<MountedTriumphSyncMessage> {
    private int entityId;

    public MountedTriumphSyncMessage() {}

    MountedTriumphSyncMessage(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        ClientMountedTriumphState.trigger(entityId);
    }

    @Override
    public MountedTriumphSyncMessage fromBytes(FriendlyByteBuf buf) {
        entityId = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }
}
