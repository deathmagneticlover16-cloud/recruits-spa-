package com.lastpagans.recruitsspa;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.ThreadLocalRandom;

/** Client-only playback for semantic execution cues. Audio must never be able to crash matched combat. */
final class ClientExecutionAudio {
    private ClientExecutionAudio() {}

    static void play(int entityId, String key, float volume, float pitchVariance) {
        try {
            Minecraft minecraft = Minecraft.m_91087_();
            ClientLevel level = minecraft == null ? null : minecraft.f_91073_;
            if (level == null) {
                return;
            }

            Entity entity = level.m_6815_(entityId);
            if (entity == null) {
                return;
            }

            ResourceLocation id = new ResourceLocation(RecruitsSeriousAnimations.MOD_ID, key);

            // Do not read BuiltInRegistries here. In 1.20.1 Registry is an interface; the
            // old compile stub produced an invokevirtual call and caused an
            // IncompatibleClassChangeError as soon as the first execution cue played.
            // SoundEvent's variable-range factory is the same path Better Combat 1.8.6
            // uses for dynamically-addressed sounds and is safe for local playback.
            SoundEvent sound = SoundEvent.m_262824_(id);

            float pitch = 1.0F;
            if (pitchVariance > 0.0F) {
                pitch += (float) ThreadLocalRandom.current().nextDouble(-pitchVariance, pitchVariance);
            }

            level.m_7785_(
                    entity.m_20185_(),
                    entity.m_20186_(),
                    entity.m_20189_(),
                    sound,
                    SoundSource.PLAYERS,
                    volume,
                    pitch,
                    false
            );
        } catch (Throwable ignored) {
            // Execution SFX are cosmetic. A missing/broken sound must never kill the client
            // or interrupt the synchronized execution state machine.
        }
    }
}
