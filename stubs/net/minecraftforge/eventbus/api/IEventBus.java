package net.minecraftforge.eventbus.api;
import java.util.function.Consumer;
public interface IEventBus {
    void register(Object object);
    <T> void addListener(Consumer<T> listener);
}
