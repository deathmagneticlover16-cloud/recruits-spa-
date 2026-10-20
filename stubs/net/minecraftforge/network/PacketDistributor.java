package net.minecraftforge.network;
public class PacketDistributor<T> {
    public static final PacketDistributor<Object> ALL = new PacketDistributor<>();
    public PacketTarget noArg() { return null; }
    public interface PacketTarget {}
}
