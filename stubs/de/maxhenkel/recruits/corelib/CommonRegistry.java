package de.maxhenkel.recruits.corelib;
import de.maxhenkel.recruits.corelib.net.Message;
import net.minecraftforge.network.simple.SimpleChannel;
public final class CommonRegistry {
    public static SimpleChannel registerChannel(String modId, String name, int version) { return null; }
    public static <T extends Message<T>> void registerMessage(SimpleChannel channel, int id, Class<T> clazz) {}
}
