package com.lastpagans.recruitsspa;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.fml.ModList;

final class EntityCompat {
    private static final String ICYS_NAMESPACE = "icys-better-horses";

    private EntityCompat() {}

    static boolean isHorseLike(Entity vehicle) {
        if (vehicle == null) {
            return false;
        }
        if (vehicle instanceof AbstractHorse) {
            return true;
        }
        String name = vehicle.getClass().getSimpleName();
        return name.contains("Horse") || name.contains("Donkey") || name.contains("Mule");
    }

    static boolean isIcysHorse(Entity vehicle) {
        if (!(vehicle instanceof AbstractHorse)) {
            return false;
        }
        // Icy mixes its gait, seat and rider-motion systems into AbstractHorse,
        // so vanilla horses are managed too while the mod is present.
        if (ModList.get().isLoaded("icys_better_horses")) {
            return true;
        }
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
        return ICYS_NAMESPACE.equals(id.getNamespace())
                || vehicle.getClass().getName().startsWith("icy.betterhorses.");
    }
}
