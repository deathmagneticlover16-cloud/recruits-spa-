package net.minecraftforge.registries;
import java.util.function.Supplier;
import net.minecraftforge.eventbus.api.IEventBus;
public class DeferredRegister<T> {
    public static <T> DeferredRegister<T> create(IForgeRegistry<T> registry, String modid) { return new DeferredRegister<>(); }
    public <I extends T> RegistryObject<I> register(String name, Supplier<? extends I> supplier) { return new RegistryObject<>(); }
    public void register(IEventBus bus) {}
}
