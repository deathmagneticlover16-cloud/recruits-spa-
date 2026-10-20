package net.minecraftforge.network.simple;
import net.minecraftforge.network.PacketDistributor;
public class SimpleChannel {
    public <MSG> void send(PacketDistributor.PacketTarget target, MSG message) {}
}
