package com.lastpagans.recruitsspa;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Semantic combat sound events.
 *
 * Animations never reference concrete .ogg files. They emit one of these cues,
 * while sounds.json/resource packs decide which actual samples belong to it.
 */
final class CombatSounds {
    static final String STAB_FLESH = "combat.stab_flesh";
    static final String WEAPON_CLASH = "combat.weapon_clash";
    static final String BODY_FALL = "combat.body_fall";
    static final String DEATH_VOCAL = "combat.death_vocal";
    static final String PAIN_VOCAL = "combat.pain_vocal";
    static final String SHIELD_HIT = "combat.shield_hit";

    private static final List<String> KEYS = List.of(
            STAB_FLESH,
            WEAPON_CLASH,
            BODY_FALL,
            DEATH_VOCAL,
            PAIN_VOCAL,
            SHIELD_HIT
    );

    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RecruitsSeriousAnimations.MOD_ID);

    static {
        for (String key : KEYS) {
            SOUNDS.register(key, () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(RecruitsSeriousAnimations.MOD_ID, key)
            ));
        }
    }

    private CombatSounds() {}

    static void register() {
        SOUNDS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
