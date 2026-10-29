package com.lastpagans.recruitsspa;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.fml.ModList;

final class EntityCompat {
    private static final String ICYS_NAMESPACE = "icys-better-horses";
    private static final boolean ICYS_LOADED = ModList.get().isLoaded("icys_better_horses");
    private static final ClassValue<Boolean> HORSE_LIKE_CLASSES = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            String name = type.getSimpleName();
            return name.contains("Horse") || name.contains("Donkey") || name.contains("Mule");
        }
    };

    private EntityCompat() {}

    static boolean isHorseLike(Entity vehicle) {
        if (vehicle == null) {
            return false;
        }
        if (vehicle instanceof AbstractHorse) {
            return true;
        }
        return HORSE_LIKE_CLASSES.get(vehicle.getClass());
    }

    static boolean isIcysHorse(Entity vehicle) {
        if (!(vehicle instanceof AbstractHorse)) {
            return false;
        }
        // Icy mixes its gait, seat and rider-motion systems into AbstractHorse,
        // so vanilla horses are managed too while the mod is present.
        if (ICYS_LOADED) {
            return true;
        }
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
        return ICYS_NAMESPACE.equals(id.getNamespace())
                || vehicle.getClass().getName().startsWith("icy.betterhorses.");
    }
}
